package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import com.google.android.gms.maps.model.LatLng
import java.io.NotActiveException

data class ViewState(
    val showWhereAreYouGoingButton: Boolean,
    val tripData: TripData?,
    val userLocation: LatLng?
)
