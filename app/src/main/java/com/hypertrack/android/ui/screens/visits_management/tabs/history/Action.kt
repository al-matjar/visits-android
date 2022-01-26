package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.history.HistoryState
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import java.time.LocalDate

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

data class MapReadyAction(val map: HypertrackMapWrapper) : Action()
data class HistoryUpdatedAction(val history: HistoryState) : Action()
object MapClickedAction : Action()
data class TimelineItemSelected(val tile: TimelineTile) : Action()
data class UserLocationReceived(val userLocation: LatLng) : Action()
object SelectDateClickedAction : Action()
data class OnDateSelectedAction(val date: LocalDate) : Action()
data class OnGeofenceClickAction(val geofenceId: String) : Action()
data class OnBottomSheetStateChangedAction(val expanded: Boolean) : Action()
data class OnErrorAction(val exception: Exception) : Action()
object OnBackPressedAction : Action()
object OnReloadPressedAction : Action()
object OnResumeAction : Action()

