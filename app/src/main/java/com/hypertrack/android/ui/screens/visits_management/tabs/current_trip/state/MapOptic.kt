package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

object MapOptic {

    fun get(state: InitializedState): HypertrackMapWrapper? {
        return when (state.trackingState) {
            is Tracking -> {
                state.trackingState.mapUiState?.map
            }
            is NotTracking -> {
                state.trackingState.map
            }
        }
    }

}
