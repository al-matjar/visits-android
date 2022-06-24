package com.hypertrack.android.api.graphql.models

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Point
import com.hypertrack.android.api.Polygon
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlGeofence(
    @field:Json(name = "geofence_id") val geofenceId: String,
    @field:Json(name = "device_id") val deviceId: String?,
    @field:Json(name = "metadata") val metadata: String,
    @field:Json(name = "geometry") val graphQlGeometry: GraphQlPointGeometry,
    @field:Json(name = "radius") val radius: Int?,
    //this field is always null and have invalid format on graphql endpoint
//    @field:Json(name = "address") val address: String?,
) {

    @Transient
    val address: String? = null

    @Transient
    private val geometry = graphQlGeometry.center?.let {
        Point(graphQlGeometry.center)
    } ?: graphQlGeometry.vertices?.let {
        Polygon(listOf(it))
    }

    @Transient
    val location: LatLng? = geometry?.let { LatLng(it.latitude, it.longitude) }

    @Transient
    val type: String? = geometry?.type
}
