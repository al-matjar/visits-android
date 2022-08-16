package com.hypertrack.android.api.models

import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.api.Views
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteTrip(
    @field:Json(name = "trip_id") val id: String?,
    @field:Json(name = "views") val views: Views?,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "started_at") val createdAt: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
    @field:Json(name = "destination") val destination: TripDestination?,
    @field:Json(name = "estimate") val estimate: RemoteEstimate?,
    @field:Json(name = "orders") val orders: List<RemoteOrder>?,
)
