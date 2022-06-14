package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.Either
import com.hypertrack.android.utils.LoadingState
import java.time.LocalDate

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class MoveMapEffect(
    val map: HypertrackMapWrapper,
    val target: MapZoomTarget
) : Effect()

typealias MapZoomTarget = Either<LatLng, LatLngBounds>

data class MoveMapToBoundsEffect(
    val map: HypertrackMapWrapper,
    val latLngBounds: LatLngBounds,
    val mapPadding: Int
) : Effect()

class SetBottomSheetStateEffect(val expanded: Boolean) : Effect()

data class UpdateMapEffect(
    val map: HypertrackMapWrapper,
    val userLocation: LatLng?,
    val mapHistoryData: MapHistoryData,
) : Effect()

data class UpdateViewStateEffect(
    val date: LocalDate,
    val showAddGeotagButton: Boolean,
    val historyData: LoadingState<HistoryData>
) : Effect()

data class ShowDatePickerDialogEffect(val date: LocalDate) : Effect()

data class LoadHistoryEffect(val userScope: UserScope, val date: LocalDate) : Effect()

data class SendErrorToCrashlytics(val exception: Exception) : Effect()

data class OpenGeotagInfoDialogEffect(val geotag: Geotag) : Effect()

data class OpenGeofenceVisitInfoDialogEffect(val visit: LocalGeofenceVisit) : Effect()

data class OpenGeofenceDetailsEffect(val geofenceId: String) : Effect()

data class IllegalActionEffect(val action: Action, val state: State) : Effect()
