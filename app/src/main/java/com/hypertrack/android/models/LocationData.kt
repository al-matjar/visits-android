package com.hypertrack.android.models

import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationData(
    val latitude: Double,
    val longitude: Double
) {
    fun toLatLng() = LatLng(
        latitude,
        longitude
    )
}
