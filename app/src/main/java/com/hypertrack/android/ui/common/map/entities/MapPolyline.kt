package com.hypertrack.android.ui.common.map.entities

import com.google.android.gms.maps.model.PolylineOptions

interface MapPolyline {
    val polylineOptions: PolylineOptions
}

@Suppress("MemberVisibilityCanBePrivate")
open class MapPolylineImpl(override val polylineOptions: PolylineOptions) : MapPolyline {
    override fun toString(): String {
        return "${javaClass.simpleName}(${polylineOptions.points.size})"
    }
}
