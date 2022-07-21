package com.hypertrack.android.ui.common.map

import android.graphics.Color
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hypertrack.android.ui.common.map.entities.GeofenceForDetailsOptions
import com.hypertrack.android.ui.common.map.entities.GeofenceVisitMarker
import com.hypertrack.android.ui.common.map.entities.GeotagMarker
import com.hypertrack.android.ui.common.map.entities.HistoryPolyline
import com.hypertrack.android.ui.common.map.entities.MapCircleOptions
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

    fun createGeofenceForDetailView(location: LatLng, radius: Int): GeofenceForDetailsOptions {
        return GeofenceForDetailsOptions(
            center = MapCircleOptions(
                CircleOptions()
                    .center(location)
                    .fillColor(style.colorGeofenceFill)
                    .strokeColor(style.colorGeofence)
                    .strokeWidth(3f)
                    .radius(radius.toDouble())
                    .visible(true)
            ),
            radius = MapCircleOptions(
                CircleOptions()
                    .center(location)
                    .fillColor(style.colorGeofence)
                    .strokeColor(Color.TRANSPARENT)
                    .radius((radius / 10).toDouble())
                    .visible(true)
            )
        )
    }

}
