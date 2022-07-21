package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.TripData

data class ViewState(
    val showWhereAreYouGoingButton: Boolean,
    val tripData: TripData?,
    val userLocation: LatLng?
)
