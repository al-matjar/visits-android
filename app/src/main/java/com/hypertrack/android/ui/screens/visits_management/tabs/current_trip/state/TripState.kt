package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.hypertrack.android.models.local.Trip

sealed class TripState {
    override fun toString(): String = javaClass.simpleName
}

data class ActiveTrip(
    val trip: Trip
) : TripState()

object NoActiveTrip : TripState()
