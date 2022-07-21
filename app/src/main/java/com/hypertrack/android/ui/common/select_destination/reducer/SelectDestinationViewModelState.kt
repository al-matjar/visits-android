package com.hypertrack.android.ui.common.select_destination.reducer

import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiState

// @formatter:off

sealed class State

data class MapNotReady(
    val userLocation: UserLocation?,
    val waitingForUserLocationMove: Boolean
) : State()

data class MapReady(
    val mapUiState: MapUiState,
    val userLocation: UserLocation?,
    val placeData: PlaceData,
    val flow: UserFlow,
    val waitingForUserLocationMove: Boolean
) : State() {
    val map: HypertrackMapWrapper
        get() = mapUiState.map
}

// @formatter:on

fun MapNotReady.withUserLocation(userLocation: UserLocation): MapNotReady {
    return copy(userLocation = userLocation)
}

fun MapReady.withPlaceSelected(place: PlaceSelected, flow: MapFlow): MapReady {
    return copy(placeData = place, flow = flow)
}

fun MapReady.withMapFlow(flow: MapFlow): MapReady {
    return copy(flow = flow)
}

fun MapReady.withAutocompleteFlow(flow: AutocompleteFlow): MapReady {
    return copy(flow = flow)
}

