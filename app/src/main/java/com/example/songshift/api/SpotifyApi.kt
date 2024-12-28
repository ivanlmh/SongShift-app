package com.example.songshift.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface SpotifyApi {
    @GET("tracks/{id}")
    suspend fun getTrack(
        @Path("id") trackId: String,
        @Header("Authorization") authorization: String
    ): SpotifyTrack
}

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