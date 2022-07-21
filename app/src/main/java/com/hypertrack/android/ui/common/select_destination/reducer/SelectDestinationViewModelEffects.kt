package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiEffect
import com.hypertrack.android.use_case.app.UserScopeUseCases

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class DisplayLocationInfo(
    val address: String,
    val placeName: String?
) : Effect()

data class Proceed(val placeData: PlaceData, val useCases: UserScopeUseCases) : Effect()
data class MoveMapToPlace(
    val placeSelected: PlaceSelected,
    val map: HypertrackMapWrapper
) : Effect()

data class MoveMapToUserLocation(
    val userLocation: UserLocation,
    val map: HypertrackMapWrapper
) : Effect()

data class AnimateMapToUserLocation(
    val userLocation: UserLocation,
    val map: HypertrackMapWrapper
) : Effect()

data class MapUiEffect(
    val effect: MapUiEffect
) : Effect()

object CloseKeyboard : Effect()
object RemoveSearchFocus : Effect()
object HideProgressbar : Effect()
object ClearSearchQuery : Effect()
