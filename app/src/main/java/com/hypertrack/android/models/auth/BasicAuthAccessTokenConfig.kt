package com.hypertrack.android.models.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BasicAuthAccessTokenConfig(
    val authUrl: String,
    val deviceId: String,
    val userName: String,
    val userPwd: String = "",
    var token: String? = null
)
