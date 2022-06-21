package com.hypertrack.android.interactors.app.effect

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapData

sealed class MapEffect
data class UpdateMapEffect(val map: HypertrackMapWrapper, val data: MapData) : MapEffect()
data class ClearMapEffect(val map: HypertrackMapWrapper) : MapEffect()
data class MoveMapToBoundsEffect(
    val map: HypertrackMapWrapper,
    val latLngBounds: LatLngBounds,
    val mapPadding: Int = DEFAULT_MAP_PADDING
) : MapEffect() {
    companion object {
        const val DEFAULT_MAP_PADDING = 50
    }
}

data class MoveMapToLocationEffect(
    val map: HypertrackMapWrapper,
    val latLng: LatLng
) : MapEffect()
