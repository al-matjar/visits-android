package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon

data class GeofenceForMap(
    val id: GeofenceId,
    val location: LatLng,
    val radius: Int,
    val isPolygon: Boolean,
    val polygon: List<LatLng>?
) {
    companion object {
        fun fromGeofence(geofence: Geofence): GeofenceForMap {
            return GeofenceForMap(
                id = geofence.id,
                location = geofence.location,
                radius = geofence.radius,
                isPolygon = geofence.isPolygon,
                polygon = geofence.polygon
            )
        }
    }
}
