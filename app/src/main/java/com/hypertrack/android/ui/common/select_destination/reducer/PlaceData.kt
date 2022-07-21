package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng

sealed class PlaceData
data class LocationSelected(
    val latLng: LatLng,
    val address: String,
) : PlaceData()

data class PlaceSelected(
    val displayAddress: String,
    val strictAddress: String?,
    val name: String?,
    val latLng: LatLng
) : PlaceData()
