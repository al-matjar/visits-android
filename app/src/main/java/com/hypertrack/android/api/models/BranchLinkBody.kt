package com.hypertrack.android.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BranchLinkBody(
    @field:Json(name = "link_url") val linkUrl: String
)
