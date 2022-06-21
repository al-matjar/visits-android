package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import java.time.LocalDate

sealed class ViewEvent {
    override fun toString(): String = javaClass.simpleName
}

data class SetBottomSheetStateEvent(
    val expanded: Boolean,
    val arrowDown: Boolean
) : ViewEvent()

data class ShowDatePickerDialogEvent(val date: LocalDate) : ViewEvent()
data class ShowGeofenceVisitDialogEvent(val visitDialog: GeofenceVisitDialog) : ViewEvent()
data class ShowGeotagDialogEvent(val geotagDialog: GeotagDialog) : ViewEvent()
