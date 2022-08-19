package com.hypertrack.android.ui.common.map_state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.GeofenceForMap
import com.hypertrack.android.models.local.Trip
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.entities.GeofenceForDetailsOptions
import com.hypertrack.android.ui.common.map.entities.MapShape

sealed class MapUiAction
data class UpdateUserLocationMapUiAction(val userLocation: LatLng?) : MapUiAction()
data class UpdateTripMapUiAction(val trip: Trip?) : MapUiAction()
data class AddGeofencesMapUiAction(val geofences: List<GeofenceForMap>) : MapUiAction()
data class OnMapMovedMapUiAction(val target: LatLng) : MapUiAction()
data class UpdateMapViewMapUiAction(val map: HypertrackMapWrapper) : MapUiAction()
data class UpdateGeofenceForDetailsMapUiAction(val geofenceShapeOptions: GeofenceForDetailsOptions?) :
    MapUiAction()

data class OnGeofenceForDetailsUpdatedMapUiAction(val geofenceForDetails: List<MapShape<*>>?) :
    MapUiAction()
