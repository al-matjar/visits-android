package com.hypertrack.android.api.models

import com.hypertrack.android.api.GeofenceMarkersResponse
import com.hypertrack.android.api.Geometry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteGeofence(
    @field:Json(name = "geofence_id") val geofence_id: String,
    @field:Json(name = "device_id") val deviceId: String?,
    @field:Json(name = "created_at") val created_at: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "markers") val marker: GeofenceMarkersResponse?,
    @field:Json(name = "radius") val radius: Int?,
    @field:Json(name = "address") val address: String?,
    @field:Json(name = "archived") val archived: Boolean?,
) {

    val latitude: Double
        get() = geometry.latitude

    val longitude: Double
        get() = geometry.longitude

    val type: String
        get() = geometry.type
}
