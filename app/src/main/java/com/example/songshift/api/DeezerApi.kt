package com.example.songshift.api

import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApi {
    @GET("search")
    suspend fun searchTrack(
        @Query("q") query: String
    ): DeezerSearchResponse
}

data class DeezerSearchResponse(
    val data: List<DeezerTrack>
)

data class DeezerTrack(
    val id: Long,
    val title: String,
    val artist: DeezerArtist,
    val duration: Int,
    val link: String
)

data class DeezerArtist(
    val id: Long,
    val name: String
)