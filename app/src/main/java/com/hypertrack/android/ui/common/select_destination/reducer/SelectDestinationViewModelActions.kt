package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.select_destination.GooglePlaceModel
import com.hypertrack.android.utils.NonEmptyList

// @formatter:off

sealed class Action

data class UserLocationReceived(val latLng: LatLng, val address: String) : Action()

data class MapReadyAction(
    val map: HypertrackMapWrapper,
    val cameraPosition: LatLng,
    val address: String
) : Action()

data class MapCameraMoved(
    val latLng: LatLng,
    val address: String,
    val cause: MapMoveCause,
    val isNearZero: Boolean,
) : Action()
    sealed class MapMoveCause
    object MovedByUser : MapMoveCause() {
        override fun toString(): String = javaClass.simpleName
    }
    object MovedToPlace : MapMoveCause() {
        override fun toString(): String = javaClass.simpleName
    }
    object MovedToUserLocation : MapMoveCause() {
        override fun toString(): String = javaClass.simpleName
    }

data class MapClicked(val latLng: LatLng, val address: String) : Action()

object ConfirmClicked : Action() {
    override fun toString(): String = javaClass.simpleName
}

data class SearchQueryChanged(val query: String, val results: NonEmptyList<GooglePlaceModel>) :
    Action()

data class AutocompleteError(
    val query: String,
) : Action()

data class PlaceSelectedAction(
    val displayAddress: String,
    val strictAddress: String?,
    val name: String?,
    val latLng: LatLng
) : Action()

// @formatter:on
