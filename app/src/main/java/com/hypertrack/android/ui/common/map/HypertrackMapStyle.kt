package com.hypertrack.android.ui.common.map

import android.graphics.Color
import android.util.TypedValue
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class HypertrackMapStyle(
    private val osUtilsProvider: OsUtilsProvider
) {
    val routePolylineWidth: Float by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f,
            osUtilsProvider.getDisplayMetrics()
        )
    }

    val routeColor: Int by lazy {
        osUtilsProvider.colorFromResource(R.color.colorHyperTrackGreen)
    }

    val geofenceMarkerIcon: BitmapDescriptor by lazy {
        BitmapDescriptorFactory.fromBitmap(
            osUtilsProvider.bitmapFromResource(
                R.drawable.ic_ht_geofence_visited_active
            )
        )
    }

    val geotagMarkerIcon: BitmapDescriptor by lazy {
        BitmapDescriptorFactory.fromBitmap(
            osUtilsProvider.bitmapFromResource(
                R.drawable.ic_geotag_marker
            )
        )
    }

    val colorGeofenceFill = osUtilsProvider.colorFromResource(R.color.colorGeofenceFill)
    val colorGeofence = osUtilsProvider.colorFromResource(R.color.colorGeofence)
    val colorHyperTrackGreenSemitransparent = osUtilsProvider.colorFromResource(
        R.color.colorHyperTrackGreenSemitransparent
    )

}
