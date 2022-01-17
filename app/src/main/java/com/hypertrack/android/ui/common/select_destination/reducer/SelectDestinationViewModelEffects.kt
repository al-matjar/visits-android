package com.hypertrack.android.ui.common.select_destination.reducer

import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

// @formatter:off

sealed class Effect
data class DisplayLocationInfo(
    val address: String,
    val placeName: String?
) : Effect()

data class Proceed(val placeData: PlaceData) : Effect()
data class MoveMapToPlace(val placeSelected: PlaceSelected, val map: HypertrackMapWrapper) : Effect()
data class MoveMapToUserLocation(val userLocation: UserLocation, val map: HypertrackMapWrapper) : Effect()
object CloseKeyboard : Effect() {
    override fun toString(): String = javaClass.simpleName
}

object RemoveSearchFocus : Effect() {
    override fun toString(): String = javaClass.simpleName
}

object HideProgressbar : Effect() {
    override fun toString(): String = javaClass.simpleName
}

object ClearSearchQuery : Effect() {
    override fun toString(): String = javaClass.simpleName
}

// @formatter:on
