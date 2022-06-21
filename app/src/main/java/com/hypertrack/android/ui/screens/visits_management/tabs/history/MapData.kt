package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.entities.GeofenceVisitMarker
import com.hypertrack.android.ui.common.map.entities.GeotagMarker
import com.hypertrack.android.ui.common.map.entities.HistoryPolyline

data class MapData(
    val userLocation: LatLng?,
    val geofenceVisits: List<GeofenceVisitMarker>,
    val geotags: List<GeotagMarker>,
    val historyPolyline: HistoryPolyline,
)
