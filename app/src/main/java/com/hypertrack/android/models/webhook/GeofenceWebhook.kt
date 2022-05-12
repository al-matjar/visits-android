package com.hypertrack.android.models.webhook

import com.hypertrack.android.api.Arrival
import com.hypertrack.android.models.GeofenceMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeofenceWebhook(
    // entry, exit
    @field:Json(name = "geofence_id") val geofenceId: String,
    @field:Json(name = "arrival") val arrival: Arrival,
    @field:Json(name = "exit") val exit: Arrival?,
    @field:Json(name = "geofence_metadata") val metadata: GeofenceMetadata?,
)
