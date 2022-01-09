package com.hypertrack.android.ui.screens.add_geotag

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class SetViewStateEffect(val viewState: ViewState) : Effect()
data class ShowErrorMessageEffect(val text: String) : Effect()
data class CreateGeotag(val metadata: Map<String, String>) : Effect()
object GoBackEffect : Effect()
data class ShowOnMapEffect(val map: HypertrackMapWrapper, val latestLocation: LatLng) : Effect()
