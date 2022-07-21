package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

object NoActiveTripStateOptic {

    fun set(state: InitializedState, trackingState: Tracking, noActiveTrip: NoActiveTrip): State {
        return state.copy(trackingState = trackingState.copy(tripState = noActiveTrip))
    }

}
