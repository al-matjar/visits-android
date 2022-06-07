package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import java.lang.Exception

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

object OnViewCreatedAction : Action()
object OnWhereAreYouGoingClickAction : Action()
object OnShareTripClickAction : Action()
object OnCompleteClickAction : Action()
object OnAddOrderClickAction : Action()
object OnMyLocationClickAction : Action()
object OnTripFocusedAction : Action()
object OnResumeAction : Action()
object OnPauseAction : Action()
object OnGeofencesUpdateAction : Action()
data class OnMapReadyAction(val context: Context, val map: HypertrackMapWrapper) : Action()
data class InitMapAction(val context: Context, val map: GoogleMap) : Action()
data class OnOrderClickAction(val orderId: String) : Action()
data class ErrorAction(val exception: Exception) : Action()
data class UserLocationChangedAction(val location: LatLng?) : Action()
data class TripUpdatedAction(val trip: LocalTrip?) : Action()
data class TrackingStateChangedAction(val isTracking: Boolean) : Action()