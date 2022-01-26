package com.hypertrack.android.ui.common.map.entities

import com.google.android.gms.maps.model.MarkerOptions

interface MapMarker {
    val markerOptions: MarkerOptions
}

open class MapMarkerImpl(override val markerOptions: MarkerOptions) : MapMarker {
    override fun toString(): String {
        return "${javaClass.simpleName}(${markerOptions.position})"
    }
}
