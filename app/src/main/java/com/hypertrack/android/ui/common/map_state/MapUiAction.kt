package com.hypertrack.android.ui.common.map_state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.entities.GeofenceForDetailsOptions
import com.hypertrack.android.ui.common.map.entities.MapCircle
import com.hypertrack.android.ui.common.map.entities.MapShape

sealed class MapUiAction
data class UpdateUserLocationMapUiAction(val userLocation: LatLng?) : MapUiAction()
data class UpdateTripMapUiAction(val trip: LocalTrip?) : MapUiAction()
data class AddGeofencesMapUiAction(val geofences: List<Geofence>) : MapUiAction()
data class OnMapMovedMapUiAction(val target: LatLng) : MapUiAction()
data class UpdateMapViewMapUiAction(val map: HypertrackMapWrapper) : MapUiAction()
data class UpdateGeofenceForDetailsMapUiAction(val geofenceShapeOptions: GeofenceForDetailsOptions?) :
    MapUiAction()

data class OnGeofenceForDetailsUpdatedMapUiAction(val geofenceForDetails: List<MapShape<*>>?) :
    MapUiAction()
