package com.hypertrack.android.repository

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Deprecated("used only for getting publishable key")
@JsonClass(generateAdapter = true)
data class AccountData(
        @field:Json(name = "pub_key") val publishableKey: String? = null,
)
