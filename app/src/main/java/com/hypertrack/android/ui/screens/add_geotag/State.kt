package com.hypertrack.android.ui.screens.add_geotag

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.ErrorMessage

sealed class State(val viewState: ViewState)

object InitialState : State(InitialViewState) {
    override fun toString(): String = javaClass.simpleName
}

data class HasLatestLocation(val latestLocation: LatLng) : State(GeotagCreationViewState())
data class OutageState(val errorMessage: ErrorMessage) : State(ErrorViewState(errorMessage))
data class ReadyForCreation(
    val map: HypertrackMapWrapper,
    val latestLocation: LatLng
) : State(GeotagCreationViewState())

