package com.example.songshift

import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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

    private fun showResult(deezerTrack: DeezerTrack) {
        currentDeezerLink = deezerTrack.link
        binding.resultContainer.visibility = View.VISIBLE
        binding.resultText.text = """
            Found match on Deezer:
            Track: ${deezerTrack.title}
            Artist: ${deezerTrack.artist.name}
            Link: ${deezerTrack.link}
        """.trimIndent()

        binding.copyLinkButton.setOnClickListener {
            copyLinkToClipboard(deezerTrack.link)
        }
    }

    private fun copyLinkToClipboard(link: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Deezer Link", link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
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
                    showResult(deezerTrack)
                } else {
                    binding.resultContainer.visibility = View.VISIBLE
                    binding.resultText.text = "No matching track found on Deezer"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching track", e)
                binding.resultContainer.visibility = View.VISIBLE
                binding.resultText.text = when (e) {
                    is HttpException -> "API Error: ${e.code()}"
                    is UnknownHostException -> "Network error. Check your internet connection."
                    else -> "Error: ${e.localizedMessage}"
                }
            } finally {
                updateUiState(isLoading = false)
            }
        }
    }
}

