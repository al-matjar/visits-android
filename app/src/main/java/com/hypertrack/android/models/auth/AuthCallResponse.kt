package com.hypertrack.android.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AuthCallResponse(
    @field:Json(name = "access_token") val accessToken: String,
    @field:Json(name = "expires_in") val expiresIn: Int
)
