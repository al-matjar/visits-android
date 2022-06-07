package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.LocalTrip

sealed class IsTrackingState
object NotTracking : IsTrackingState()
data class Tracking(
    val userLocation: LatLng?,
    val trip: LocalTrip?,
) : IsTrackingState()
