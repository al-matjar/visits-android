package com.hypertrack.android.api.graphql.models

import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.datetime.TimeValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlHistory(
    @field:Json(name = "geofence_markers") val visits: List<GraphQlGeofenceVisit>,
    @field:Json(name = "device_status_markers") val deviceStatusMarkers: List<GraphQlDeviceStatusMarker>,
    @field:Json(name = "geotag_markers") val geotagMarkers: List<GraphQlGeotagMarker>,
    @field:Json(name = "locations") val locations: List<RemoteLocation>,
    // todo for some reason there is no total distance in GraphQl API
//    @field:Json(name = "distance") val totalDistance: Int,
    // todo for some reason there is no total duration in GraphQl API
//    @field:Json(name = "duration") val totalDuration: Int,
    @field:Json(name = "steps") val stepsCount: Int,
    @field:Json(name = "walk_duration") val totalWalkDuration: Int,
    @field:Json(name = "stop_duration") val totalStopDuration: Int,
    @field:Json(name = "drive_distance") val totalDriveDistanceMeters: Int,
    @field:Json(name = "drive_duration") val totalDriveDurationMinutes: Int,
)
