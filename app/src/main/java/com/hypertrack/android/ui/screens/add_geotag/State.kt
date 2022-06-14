package com.hypertrack.android.ui.screens.add_geotag

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.ErrorMessage
import java.lang.Exception

sealed class State

object InitialState : State() {
    override fun toString(): String = javaClass.simpleName
}

data class HasLatestLocation(val latestLocation: LatLng) : State()
data class OutageState(val outageText: String) : State()
data class ReadyForCreation(
    val map: HypertrackMapWrapper,
    val latestLocation: LatLng
) : State()

