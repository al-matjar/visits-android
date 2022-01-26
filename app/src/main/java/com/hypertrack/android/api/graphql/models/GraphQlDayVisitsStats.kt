package com.hypertrack.android.api.graphql.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlDayVisitsStats(
    @field:Json(name = "geofence_markers") val visits: List<GraphQlGeofenceVisit>,
    @field:Json(name = "drive_distance") val totalDriveDistanceMeters: Int
) {
    fun isEmpty() = visits.isEmpty() && totalDriveDistanceMeters == 0
}
