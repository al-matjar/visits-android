package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state

import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.use_case.sdk.TrackingState

object MapUiStateOptic {

    fun set(state: InitializedState, trackingState: Tracking, mapUiState: MapUiState): State {
        return state.copy(trackingState = trackingState.copy(mapUiState = mapUiState))
    }

}
