package com.hypertrack.android.api.graphql.models

import com.google.gson.annotations.JsonAdapter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlAddress(
    @field:Json(name = "address") val address: String,
    @field:Json(name = "place") val placeName: String?,
)
