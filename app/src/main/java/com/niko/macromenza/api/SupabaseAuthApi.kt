package com.niko.macromenza.api

import com.niko.macromenza.model.SupabaseAuthRequest
import com.niko.macromenza.model.SupabaseAuthResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun registracija(
        @Header("apikey") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: SupabaseAuthRequest
    ): SupabaseAuthResponse

    @POST("auth/v1/token")
    suspend fun prijava(
        @Header("apikey") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabaseAuthRequest
    ): SupabaseAuthResponse
}