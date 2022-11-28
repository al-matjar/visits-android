package com.hypertrack.android.ui.screens.add_place_info

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError
import com.hypertrack.android.utils.state_machine.ReducerResult

sealed class State {
    override fun toString(): String = javaClass.simpleName
}
object Initial : State()
data class Initialized(
    val mapUiState: MapUiState,
    val location: LatLng,
    val integrations: IntegrationsState,
    val address: String?,
    val radius: Int?
) : State() {
    val map: HypertrackMapWrapper
        get() = mapUiState.map
}

data class CheckingForAdjacentGeofence(val previousState: Initialized) : State()

data class CreatingGeofence(val previousState: Initialized) : State()

data class ErrorState(
    val previousState: State,
    val error: DisplayableError
) : State()
