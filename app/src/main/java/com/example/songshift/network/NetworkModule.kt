package com.example.songshift.network

import android.util.Base64
import com.example.songshift.api.DeezerApi
import com.example.songshift.api.SpotifyApi
import com.example.songshift.api.SpotifyAuthApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val SPOTIFY_BASE_URL = "https://api.spotify.com/v1/"
    private const val SPOTIFY_AUTH_URL = "https://accounts.spotify.com/"
    private const val DEEZER_BASE_URL = "https://api.deezer.com/"

    private const val SPOTIFY_CLIENT_ID = ""
    private const val SPOTIFY_CLIENT_SECRET = ""

    private val spotifyAuthApi = Retrofit.Builder()
        .baseUrl(SPOTIFY_AUTH_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyAuthApi::class.java)

    private val spotifyRetrofit = Retrofit.Builder()
        .baseUrl(SPOTIFY_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val deezerRetrofit = Retrofit.Builder()
        .baseUrl(DEEZER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val spotifyApi: SpotifyApi = spotifyRetrofit.create(SpotifyApi::class.java)
    val deezerApi: DeezerApi = deezerRetrofit.create(DeezerApi::class.java)

    suspend fun getSpotifyAccessToken(): String {
        val credentials = "$SPOTIFY_CLIENT_ID:$SPOTIFY_CLIENT_SECRET".toByteArray()
        val base64Credentials = Base64.encodeToString(credentials, Base64.NO_WRAP)

        val response = spotifyAuthApi.getToken(
            authorization = "Basic $base64Credentials",
            grantType = "client_credentials"
        )

        return "Bearer ${response.accessToken}"
    }
}