package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import java.time.LocalDate

sealed class HistoryViewAction {
    override fun toString(): String = javaClass.simpleName
}

data class ViewReadyAction(
    val map: HypertrackMapWrapper,
    val viewEventHandle: MutableLiveData<Consumable<ViewEvent>>
) : HistoryViewAction()
object MapClickedAction : HistoryViewAction()
data class TimelineItemSelected(val tile: TimelineTile) : HistoryViewAction()
object SelectDateClickedAction : HistoryViewAction()
data class OnDateSelectedAction(val date: LocalDate) : HistoryViewAction()
data class OnGeofenceClickAction(val geofenceId: String) : HistoryViewAction()
data class OnBottomSheetStateChangedAction(val expanded: Boolean) : HistoryViewAction()
object OnBackPressedAction : HistoryViewAction()
object OnReloadPressedAction : HistoryViewAction()
object OnTimelineHeaderClickAction : HistoryViewAction()
object OnScrimClickAction : HistoryViewAction()
object OnResumeAction : HistoryViewAction()

