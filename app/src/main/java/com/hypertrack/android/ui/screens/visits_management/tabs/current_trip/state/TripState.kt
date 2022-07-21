package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class TripState
data class ActiveTrip(
    val trip: LocalTrip
) : TripState()

object NoActiveTrip : TripState()
