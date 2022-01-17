package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.select_destination.GooglePlaceModel
import com.hypertrack.android.utils.NonEmptyList

// @formatter:off

sealed class State

data class MapNotReady(
    val userLocation: UserLocation?,
    val waitingForUserLocationMove: Boolean
) : State()

data class UserLocation(
    val latLng: LatLng,
    val address: String
)

data class MapReady(
    val map: HypertrackMapWrapper,
    val userLocation: UserLocation?,
    val placeData: PlaceData,
    val flow: UserFlow,
    val waitingForUserLocationMove: Boolean
) : State()

sealed class PlaceData
data class PlaceSelected(
    val displayAddress: String,
    val strictAddress: String?,
    val name: String?,
    val latLng: LatLng
) : PlaceData()

data class LocationSelected(
    val latLng: LatLng,
    val address: String,
) : PlaceData()

sealed class UserFlow
object MapFlow : UserFlow() {
    override fun toString(): String = javaClass.simpleName
}

data class AutocompleteFlow(
    val places: NonEmptyList<GooglePlaceModel>
) : UserFlow()

// @formatter:on

fun MapNotReady.withUserLocation(userLocation: UserLocation): MapNotReady {
    return copy(userLocation = userLocation)
}

fun MapReady.withUserLocation(userLocation: UserLocation): MapReady {
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

