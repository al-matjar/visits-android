package com.hypertrack.android.ui.common.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hypertrack.android.ui.common.map.entities.GeofenceVisitMarker
import com.hypertrack.android.ui.common.map.entities.GeotagMarker
import com.hypertrack.android.ui.common.map.entities.HistoryPolyline
import com.hypertrack.android.ui.common.map.entities.MapMarkerImpl
import com.hypertrack.android.ui.common.map.entities.MapPolylineImpl
import com.hypertrack.android.utils.OsUtilsProvider

class HypertrackMapItemsFactory(
    private val osUtilsProvider: OsUtilsProvider
) {
    private val style = HypertrackMapStyle(osUtilsProvider)

    fun createHistoryPolyline(polyline: List<LatLng>): HistoryPolyline {
        return object : MapPolylineImpl(
            PolylineOptions()
                .width(style.routePolylineWidth)
                .color(style.routeColor)
                .addAll(polyline)
        ), HistoryPolyline {}
    }

    fun createGeotagMarker(location: LatLng): GeotagMarker {
        return object : MapMarkerImpl(
            MarkerOptions()
                .position(location)
                .icon(style.geotagMarkerIcon)
        ), GeotagMarker {}
    }

    fun createGeofenceVisitMarker(location: LatLng): GeofenceVisitMarker {
        return object : MapMarkerImpl(
            MarkerOptions()
                .position(location)
                .icon(style.geofenceMarkerIcon)
                .anchor(0.5f, 0.5f)
        ), GeofenceVisitMarker {}
    }

}


