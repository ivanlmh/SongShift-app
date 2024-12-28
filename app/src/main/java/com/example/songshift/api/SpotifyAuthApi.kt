package com.example.songshift.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface SpotifyAuthApi {
    @POST("api/token")
    @FormUrlEncoded
    suspend fun getToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String
    ): TokenResponse
}

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String
)