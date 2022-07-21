package com.hypertrack.android.ui.common.map_state

import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.withEffects

class MapUiReducer {

    fun reduce(
        action: MapUiAction,
        state: MapUiState
    ): ReducerResult<MapUiState, MapUiEffect> {
        return when (action) {
            is AddGeofencesMapUiAction -> {
                val existent = state.geofences
                val new = action.geofences
                val toAdd = new.filter { newItem ->
                    !existent.any { oldItem ->
                        oldItem.id == newItem.id
                    }
                }
                state.copy(
                    geofences = state.geofences + toAdd
                ).withEffects(AddGeofencesOnMapEffect(state.map, toAdd))
            }
            is OnMapMovedMapUiAction -> {
                state.withEffects(TriggerLoadingGeofencesEffect(action.target))
            }
            is UpdateMapViewMapUiAction -> {
                state.copy(map = action.map).withEffects(
                    UpdateMapStateEffect(state.map, state)
                )
            }
            is UpdateTripMapUiAction -> {
                state.copy(trip = action.trip).withEffects(
                    UpdateMapStateEffect(state.map, state)
                )
            }
            is UpdateUserLocationMapUiAction -> {
                state.copy(userLocation = action.userLocation).withEffects(
                    UpdateMapStateEffect(state.map, state)
                )
            }
            is UpdateGeofenceForDetailsMapUiAction -> {
                state.copy(geofenceForDetails = null).withEffects(
                    UpdateGeofenceForDetailsEffect(
                        state.map,
                        action.geofenceShapeOptions,
                        state.geofenceForDetails
                    )
                )
            }
            is OnGeofenceForDetailsUpdatedMapUiAction -> {
                state.copy(geofenceForDetails = action.geofenceForDetails).withEffects()
            }
        }
    }

}
