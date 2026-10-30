package com.niko.macromenza.model

data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

data class SupabaseOsvjeziRequest(
    @com.google.gson.annotations.SerializedName("refresh_token")
    val refreshToken: String
)

data class SupabaseAuthResponse(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Int?,
    val refresh_token: String?,
    val user: SupabaseUser?
)

data class SupabaseUser(
    val id: String,
    val email: String?
)