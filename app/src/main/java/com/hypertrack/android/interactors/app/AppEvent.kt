package com.hypertrack.android.interactors.app

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.use_case.sdk.TrackingState

sealed class AppEvent
data class UpdateUserLocationEvent(val userLocation: LatLng?) : AppEvent()
data class TrackingStateChangedEvent(val trackingState: TrackingState) : AppEvent()
data class GeofencesForMapUpdatedEvent(val geofences: List<Geofence>) : AppEvent()
