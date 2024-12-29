package com.example.songshift

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.provider.Settings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.songshift.databinding.ActivityMainBinding
import com.example.songshift.network.NetworkModule
import com.example.songshift.api.DeezerTrack
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle both VIEW and SEND intents
        handleIntent(intent)

        // Handle regular button clicks
        binding.convertButton.setOnClickListener {
            val spotifyUrl = binding.urlInput.text.toString()
            if (spotifyUrl.isBlank()) {
                binding.urlInputLayout.error = "Please enter a Spotify URL"
                return@setOnClickListener
            }

            // Try to extract track or album ID
            val trackId = extractSpotifyId(spotifyUrl, "track")
            val albumId = extractSpotifyId(spotifyUrl, "album")

            when {
                trackId != null -> searchTrack(trackId)
                albumId != null -> searchAlbum(albumId)
                else -> binding.urlInputLayout.error = "Invalid Spotify URL format"
            }
        }

        if (isFirstRun()) {
            showLinkHandlingHelp()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_app_settings -> {
                openAppSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openAppSettings() {
        try {
            // Open links settings directly
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                // Try to open directly to the app links section if possible
                action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                data = Uri.parse("package:$packageName")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to regular app settings if the direct link fails
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(fallbackIntent)
        }
    }

    // Also add this method to help users who have trouble with links
    private fun showLinkHandlingHelp() {
        AlertDialog.Builder(this)
            .setTitle("Link Handling Setup")
            .setMessage("To open Spotify links directly in this app:\n\n" +
                    "1. Go to App Settings (menu icon → App Settings)\n" +
                    "2. Find and tap 'Open by default' or 'Set as default'\n" +
                    "3. Enable 'Open supported links'\n\n" +
                    "If Spotify links still open in your browser, you may need to:\n" +
                    "1. Clear defaults for your browser app\n" +
                    "2. Or select 'Always ask' for Spotify links")
            .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

//        // debug logging here too
//        Log.d("MainActivity", "onNewIntent called")
//        intent?.let {
//            Log.d("MainActivity", "New intent: ${it.action}")
//            Log.d("MainActivity", "Intent data: ${it.data}")
//            Log.d("MainActivity", "Intent categories: ${it.categories}")
//            Log.d("MainActivity", "Intent type: ${it.type}")
//        }

        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri?.host == "open.spotify.com") {
                    when (uri.pathSegments.firstOrNull()) {
                        "track" -> {
                            val trackId = uri.pathSegments.getOrNull(1)
                            if (trackId != null) {
                                binding.urlInput.setText(uri.toString())
                                searchTrack(trackId)
                            }
                        }
                        "album" -> {
                            val albumId = uri.pathSegments.getOrNull(1)
                            if (albumId != null) {
                                binding.urlInput.setText(uri.toString())
                                searchAlbum(albumId)
                            }
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        val trackId = extractSpotifyId(sharedText, "track")
                        val albumId = extractSpotifyId(sharedText, "album")
                        when {
                            trackId != null -> {
                                binding.urlInput.setText(sharedText)
                                searchTrack(trackId)
                            }
                            albumId != null -> {
                                binding.urlInput.setText(sharedText)
                                searchAlbum(albumId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractSpotifyId(url: String, type: String): String? {
        val regex = "$type/(\\w+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun updateUiState(isLoading: Boolean) {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.convertButton.isEnabled = !isLoading
        binding.urlInput.isEnabled = !isLoading
    }


    private var currentDeezerLink: String? = null

    private fun showResult(deezerTrack: DeezerTrack, trackName: String, artistName: String) {
        currentDeezerLink = deezerTrack.link
        binding.resultContainer.visibility = View.VISIBLE

        // Get YouTube Music and YouTube search URLs
        val youtubeMusicUrl = NetworkModule.getYouTubeMusicSearchUrl(trackName, artistName)
        val youtubeUrl = NetworkModule.getYouTubeSearchUrl(trackName, artistName)

        // Show track info
        binding.resultInfoText.text = """
            Found match:
            Track: ${deezerTrack.title}
            Artist: ${deezerTrack.artist.name}
            
            Links:
        """.trimIndent()

        // Set clickable links
        binding.deezerLinkText.text = "Deezer: ${deezerTrack.link}"
        binding.youtubeMusicLinkText.text = "YouTube Music: $youtubeMusicUrl"
        binding.youtubeLinkText.text = "YouTube: $youtubeUrl"

        // Set up copy buttons for each platform
        binding.copyDeezerButton.setOnClickListener {
            copyLinkToClipboard(deezerTrack.link, "Deezer")
        }

        binding.copyYoutubeMusicButton.setOnClickListener {
            copyLinkToClipboard(youtubeMusicUrl, "YouTube Music")
        }

        binding.copyYoutubeButton.setOnClickListener {
            copyLinkToClipboard(youtubeUrl, "YouTube")
        }

        // Make links clickable with custom handling
        setupClickableLink(binding.deezerLinkText, deezerTrack.link)
        setupClickableLink(binding.youtubeMusicLinkText, youtubeMusicUrl)
        setupClickableLink(binding.youtubeLinkText, youtubeUrl)
    }

    private fun setupClickableLink(textView: TextView, url: String) {
        textView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchTrack(trackId: String) {
        lifecycleScope.launch {
            try {
                updateUiState(isLoading = true)
                binding.resultContainer.visibility = View.GONE

                val accessToken = NetworkModule.getSpotifyAccessToken()
                val spotifyTrack = NetworkModule.spotifyApi.getTrack(trackId, accessToken)

                val searchQuery = "${spotifyTrack.name} ${spotifyTrack.artists.first().name}"
                val deezerResults = NetworkModule.deezerApi.searchTrack(searchQuery)

                if (deezerResults.data.isNotEmpty()) {
                    val deezerTrack = deezerResults.data.first()
                    showResult(deezerTrack, spotifyTrack.name, spotifyTrack.artists.first().name)
                } else {
                    showNoResultsFound()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching track", e)
                showError(e)
            } finally {
                updateUiState(isLoading = false)
            }
        }
    }

    private fun searchAlbum(albumId: String) {
        lifecycleScope.launch {
            try {
                updateUiState(isLoading = true)
                binding.resultContainer.visibility = View.GONE

                val accessToken = NetworkModule.getSpotifyAccessToken()
                val spotifyAlbum = NetworkModule.spotifyApi.getAlbum(albumId, accessToken)

                // Search for first track in album as sample
                val searchQuery = "${spotifyAlbum.name} ${spotifyAlbum.artists.first().name}"
                val deezerResults = NetworkModule.deezerApi.searchTrack(searchQuery)

                if (deezerResults.data.isNotEmpty()) {
                    showResult(deezerResults.data.first(),
                        spotifyAlbum.name,
                        spotifyAlbum.artists.first().name)
                } else {
                    showNoResultsFound()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching album", e)
                showError(e)
            } finally {
                updateUiState(isLoading = false)
            }
        }
    }

    private fun showNoResultsFound() {
        binding.resultContainer.visibility = View.VISIBLE
        binding.resultInfoText.text = "No matching track found on Deezer"
        binding.deezerLinkText.text = ""
        binding.youtubeMusicLinkText.text = ""
        binding.youtubeLinkText.text = ""
    }

    private fun showError(e: Exception) {
        binding.resultContainer.visibility = View.VISIBLE
        binding.resultInfoText.text = when (e) {
            is HttpException -> "API Error: ${e.code()}"
            is UnknownHostException -> "Network error. Check your internet connection."
            else -> "Error: ${e.localizedMessage}"
        }
        binding.deezerLinkText.text = ""
        binding.youtubeMusicLinkText.text = ""
        binding.youtubeLinkText.text = ""
    }

    private fun copyLinkToClipboard(link: String, platform: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("$platform Link", link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$platform link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Add this method to your class:
    private fun isFirstRun(): Boolean {
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        if (isFirstRun) {
            prefs.edit().putBoolean("is_first_run", false).apply()
        }
        return isFirstRun
    }

}

