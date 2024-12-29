package com.example.songshift.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface SpotifyApi {
    // Your existing getTrack method
    @GET("tracks/{id}")
    suspend fun getTrack(
        @Path("id") trackId: String,
        @Header("Authorization") authorization: String
    ): SpotifyTrack

    // Add this new method for albums
    @GET("albums/{id}")
    suspend fun getAlbum(
        @Path("id") albumId: String,
        @Header("Authorization") authorization: String
    ): SpotifyAlbum
}

// Add these data classes
data class SpotifyAlbum(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val total_tracks: Int,
    val tracks: SpotifyPagingObject
)

data class SpotifyPagingObject(
    val items: List<SpotifyTrack>,
    val total: Int
)
data class SpotifyTracks(
    val items: List<SpotifyTrack>
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val duration_ms: Long
)

data class SpotifyArtist(
    val id: String,
    val name: String
)