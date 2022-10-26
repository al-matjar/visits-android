package com.hypertrack.android.interactors.app

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.GeofenceForMap
import com.hypertrack.android.use_case.sdk.TrackingState
import com.hypertrack.android.utils.message.AppMessage

sealed class AppEvent
data class AppMessageEvent(val message: String) : AppEvent()
data class UpdateUserLocationEvent(val userLocation: LatLng?) : AppEvent()
data class TrackingStateChangedEvent(val trackingState: TrackingState) : AppEvent()
data class GeofencesForMapUpdatedEvent(val geofences: List<GeofenceForMap>) : AppEvent() {
    override fun toString(): String {
        return "${javaClass.simpleName}(geofences=${geofences.size})"
    }
}
