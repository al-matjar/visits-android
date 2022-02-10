package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.entities.GeofenceVisitMarker
import com.hypertrack.android.ui.common.map.entities.GeotagMarker
import com.hypertrack.android.ui.common.map.entities.HistoryPolyline
import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.datetime.TimeValue
import java.time.LocalDate

sealed class State {
    override fun toString(): String = javaClass.simpleName
}

data class Initial(
    val date: LocalDate,
    val historyData: LoadingState<HistoryData>,
    val userLocation: LatLng?
) : State()

data class MapReadyState(
    val date: LocalDate,
    val map: HypertrackMapWrapper,
    val historyData: LoadingState<HistoryData>,
    val userLocation: LatLng?,
    val bottomSheetExpanded: Boolean
) : State()

data class HistoryData(
    val mapData: MapHistoryData,
    val timelineTiles: List<TimelineTile>,
    val summary: DaySummary
)

data class DaySummary(
    val totalDriveDistance: DistanceValue,
    val totalDriveDuration: TimeValue
) {
    fun isZero() = totalDriveDistance.meters == 0 && totalDriveDuration.totalSeconds == 0L
}

data class MapHistoryData(
    val geofenceVisits: List<GeofenceVisitMarker>,
    val geotags: List<GeotagMarker>,
    val historyPolyline: HistoryPolyline,
    val segmentSelection: SegmentSelection
)

class Timeline()

sealed class SegmentSelection {
    override fun toString(): String = javaClass.simpleName
}

object NotSelected : SegmentSelection()
class SelectedSegment(
    val polylineSegment: PolylineOptions,
    val edgeMarkers: List<MarkerOptions>
) : SegmentSelection()

fun withSegmentSelection(
    state: MapReadyState,
    historyData: LoadingSuccess<HistoryData>,
    segmentSelection: SegmentSelection
): State {
    return state.copy(historyData = historyData.map {
        it.copy(
            mapData = it.mapData.copy(
                segmentSelection = segmentSelection
            )
        )
    })
}


