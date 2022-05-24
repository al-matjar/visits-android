package com.hypertrack.android.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LiveAppBackendNotification(
    val type: String,
    val data: Map<String, Any>?
)
