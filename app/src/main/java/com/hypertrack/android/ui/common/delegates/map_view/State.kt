package com.hypertrack.android.ui.common.delegates.map_view

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

sealed class State {
    override fun toString(): String = javaClass.simpleName
}

data class Initial(
    val map: GoogleMap? = null,
    val mapView: MapView? = null
) : State()

data class Attached(
    val map: GoogleMap? = null,
    val mapView: MapView? = null
) : State()

data class MapReady(val mapView: MapView, val map: GoogleMap) : State() {
    override fun toString(): String = javaClass.simpleName
}
