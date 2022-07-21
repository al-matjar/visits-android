package com.hypertrack.android.ui.common.map.entities

sealed class HypertrackMapEntity(val shapes: List<MapShapeOptions<*>>) {
    override fun toString(): String = javaClass.simpleName
}

class GeofenceForDetailsOptions(center: MapCircleOptions, radius: MapCircleOptions) :
    HypertrackMapEntity(
        listOf(center, radius)
    )

