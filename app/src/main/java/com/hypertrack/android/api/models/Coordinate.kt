package com.hypertrack.android.api.models

import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
) {
    fun toLatLng() = LatLng(latitude, longitude)
}
