package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiState

sealed class IsTrackingState {
    override fun toString(): String = javaClass.simpleName
}

data class NotTracking(
    val map: HypertrackMapWrapper?
) : IsTrackingState()

data class Tracking(
    val mapUiState: MapUiState?,
    val userLocation: LatLng?,
    val tripState: TripState,
) : IsTrackingState()

