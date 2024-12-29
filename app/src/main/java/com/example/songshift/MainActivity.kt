package com.example.songshift

import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.TextView
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

            val trackId = extractSpotifyId(spotifyUrl)
            if (trackId != null) {
                searchTrack(trackId)
            } else {
                binding.urlInputLayout.error = "Invalid Spotify URL format"
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                // Handle URLs (existing functionality)
                val uri = intent.data
                if (uri?.host == "open.spotify.com" && uri.pathSegments.firstOrNull() == "track") {
                    val trackId = uri.pathSegments.getOrNull(1)
                    if (trackId != null) {
                        binding.urlInput.setText(uri.toString())
                        searchTrack(trackId)
                    }
                }
            }

            Intent.ACTION_SEND -> {
                // Handle shared content
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        val trackId = extractSpotifyId(sharedText)
                        if (trackId != null) {
                            binding.urlInput.setText(sharedText)
                            searchTrack(trackId)
                        }
                    }
                }
            }
        }
    }

    private fun updateUiState(isLoading: Boolean) {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.convertButton.isEnabled = !isLoading
        binding.urlInput.isEnabled = !isLoading
    }

    private fun extractSpotifyId(url: String): String? {
        val regex = "track/(\\w+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
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
}

