package com.hypertrack.android.ui.common.map_state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.GeofenceForMap
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.entities.GeofenceForDetailsOptions
import com.hypertrack.android.ui.common.map.entities.HypertrackMapEntity
import com.hypertrack.android.ui.common.map.entities.MapCircle
import com.hypertrack.android.ui.common.map.entities.MapShape
import com.hypertrack.android.ui.common.map.entities.MapShapeOptions
import com.hypertrack.android.ui.screens.add_place_info.Effect

sealed class MapUiEffect
data class AddGeofencesOnMapEffect(
    val map: HypertrackMapWrapper,
    val geofences: List<GeofenceForMap>
) : MapUiEffect()

data class UpdateMapStateEffect(
    val map: HypertrackMapWrapper,
    val mapUiState: MapUiState
) : MapUiEffect()

data class UpdateGeofenceForDetailsEffect(
    val map: HypertrackMapWrapper,
    val geofenceForDetails: GeofenceForDetailsOptions?,
    val oldGeofenceForDetails: List<MapShape<*>>?
) : MapUiEffect()

data class TriggerLoadingGeofencesEffect(val latLng: LatLng) : MapUiEffect()
data class TriggerLoadingGeofencesForMapPositionEffect(val map: HypertrackMapWrapper) :
    MapUiEffect()
