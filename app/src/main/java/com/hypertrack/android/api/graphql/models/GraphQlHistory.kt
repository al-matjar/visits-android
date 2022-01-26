package com.hypertrack.android.api.graphql.models

import com.hypertrack.android.api.models.RemoteLocation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlHistory(
    @field:Json(name = "geofence_markers") val visits: List<GraphQlGeofenceVisit>,
    @field:Json(name = "device_status_markers") val deviceStatusMarkers: List<GraphQlDeviceStatusMarker>,
    @field:Json(name = "geotag_markers") val geotagMarkers: List<GraphQlGeotagMarker>,
    @field:Json(name = "locations") val locations: List<RemoteLocation>,
    @field:Json(name = "drive_distance") val totalDriveDistanceMeters: Int,
    @field:Json(name = "drive_duration") val totalDriveDurationMinutes: Int,
)
