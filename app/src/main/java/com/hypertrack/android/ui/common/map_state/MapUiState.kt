package com.hypertrack.android.ui.common.map_state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.entities.MapCircle
import com.hypertrack.android.ui.common.map.entities.MapShape

data class MapUiState(
    val map: HypertrackMapWrapper,
    val trip: LocalTrip? = null,
    val userLocation: LatLng? = null,
    val geofences: Set<Geofence> = setOf(),
    val geofenceForDetails: List<MapShape<*>>? = null
)


