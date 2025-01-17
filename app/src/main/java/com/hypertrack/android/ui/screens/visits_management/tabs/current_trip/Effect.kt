package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import androidx.navigation.NavDirections
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.local.Trip
import com.hypertrack.android.ui.common.map_state.MapUiEffect
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.ViewState
import java.lang.Exception

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class UpdateViewStateEffect(val viewState: ViewState) : Effect()
data class SetMapPaddingEffect(val map: HypertrackMapWrapper, val bottomPadding: Int) : Effect()
data class ErrorEffect(val exception: Exception) : Effect()

data class ClearAndMoveMapEffect(val map: HypertrackMapWrapper, val position: LatLng) : Effect()
data class MoveMapEffect(val map: HypertrackMapWrapper, val position: LatLng) : Effect()
data class SetMapActiveStateEffect(val map: HypertrackMapWrapper, val active: Boolean) : Effect()
data class AnimateMapEffect(val map: HypertrackMapWrapper, val position: LatLng) : Effect()
data class AnimateMapToTripEffect(
    val map: HypertrackMapWrapper,
    val trip: Trip,
    val userLocation: LatLng?
) : Effect()

data class ClearMapEffect(val map: HypertrackMapWrapper) : Effect()
data class SubscribeOnUserScopeEventsEffect(val userScope: UserScope) : Effect()
data class RefreshTripsEffect(val userScope: UserScope) : Effect()

data class CompleteTripEffect(
    val tripId: String,
    val map: HypertrackMapWrapper?,
    val userScope: UserScope
) : Effect()

data class NavigateEffect(val destination: NavDirections) : Effect()
data class ShareTripLinkEffect(val url: String) : Effect()
data class PrepareMapEffect(
    val context: Context,
    val map: GoogleMap,
    val userScope: UserScope
) : Effect()

data class ProceedTripCreationEffect(
    val destinationData: DestinationData,
    val userScope: UserScope
) : Effect()

data class StartTripUpdateTimer(val userScope: UserScope) : Effect()
data class StopTripUpdateTimer(val userScope: UserScope) : Effect()

data class MapUiEffect(val effect: MapUiEffect) : Effect()

