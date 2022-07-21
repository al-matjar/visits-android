package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng

data class UserLocation(
    val latLng: LatLng,
    val address: String
)
