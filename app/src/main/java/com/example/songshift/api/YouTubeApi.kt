package com.example.songshift.api

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>
)

data class YouTubeSearchItem(
    val id: YouTubeVideoId,
    val snippet: YouTubeSnippet
)

data class YouTubeVideoId(
    val videoId: String
)

data class YouTubeSnippet(
    val title: String,
    val channelTitle: String
)