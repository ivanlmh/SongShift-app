package com.example.songshift

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.songshift.databinding.ActivityMainBinding
import com.example.songshift.network.NetworkModule
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.convertButton.setOnClickListener {
            val spotifyUrl = binding.urlInput.text.toString()
            val trackId = extractSpotifyId(spotifyUrl)
            if (trackId != null) {
                searchTrack(trackId)
            }
        }
    }

    private fun extractSpotifyId(url: String): String? {
        val regex = "track/(\\w+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun searchTrack(trackId: String) {
        lifecycleScope.launch {
            try {
                // Get access token
                val accessToken = NetworkModule.getSpotifyAccessToken()

                // Use token to get track
                val spotifyTrack = NetworkModule.spotifyApi.getTrack(
                    trackId,
                    accessToken
                )

                val searchQuery = "${spotifyTrack.name} ${spotifyTrack.artists.first().name}"
                val deezerResults = NetworkModule.deezerApi.searchTrack(searchQuery)

                if (deezerResults.data.isNotEmpty()) {
                    val deezerTrack = deezerResults.data.first()
                    binding.resultText.text = "Found on Deezer:\n${deezerTrack.link}"
                } else {
                    binding.resultText.text = "No matching track found on Deezer"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching track", e)
                binding.resultText.text = "Error: ${e.localizedMessage}"
            }
        }
    }
}