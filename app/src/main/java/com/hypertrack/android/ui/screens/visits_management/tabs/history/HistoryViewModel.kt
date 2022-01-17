package com.hypertrack.android.ui.screens.visits_management.tabs.history

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.hypertrack.android.interactors.HistoryInteractor
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter

import com.hypertrack.logistics.android.github.R

class HistoryViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val historyInteractor: HistoryInteractor,
    private val datetimeFormatter: DatetimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val deviceLocationProvider: DeviceLocationProvider,
) : BaseViewModel(baseDependencies) {
    //todo remove legacy
    private val timeDistanceFormatter = TimeDistanceFormatter(
        datetimeFormatter,
        distanceFormatter
    )

    val style = BaseHistoryStyle(MyApplication.context)

    override val errorHandler = ErrorHandler(
        osUtilsProvider,
        crashReportsProvider,
        historyInteractor.errorFlow.asLiveData()
    )

    //value doesn't represent correct state
    val bottomSheetOpened = MutableLiveData<Boolean>(false)

    val tiles = MediatorLiveData<List<HistoryTile>>()

    private val history = historyInteractor.todayHistory
    private var userLocation: LatLng? = null
    private var map: HypertrackMapWrapper? = null

    private var selectedSegment: Polyline? = null
    private var viewBounds: LatLngBounds? = null
    private val activeMarkers = mutableListOf<Marker>()

    private var firstMovePerformed = false

    init {
        loadingState.postValue(true)

        tiles.addSource(history) {
            it?.let {
                if (it.locationTimePoints.isNotEmpty()) {
                    Log.d(TAG, "got new history $it")
                    val asTiles = historyToTiles(it, timeDistanceFormatter)
                    Log.d(TAG, "converted to tiles $asTiles")
                    tiles.postValue(asTiles)
                } else {
                    Log.d(TAG, "Empty history")
                    tiles.postValue(emptyList())
                }
            }
        }

        history.observeManaged {
            it?.let {
                loadingState.postValue(false)
                val map = this.map
                if (map != null) {
                    displayHistory(map, it)
                    moveMap(map, it, userLocation)
                }
            }
        }

        deviceLocationProvider.getCurrentLocation {
            userLocation = it?.toLatLng()
            history.value?.let { hist ->
                map?.let { map ->
                    moveMap(map, hist, it?.toLatLng())
                }
            }
        }
    }

    fun onMapReady(context: Context, googleMap: GoogleMap) {
        firstMovePerformed = false
        val style = BaseHistoryStyle(MyApplication.context)
        this.map = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = true,
                enableMyLocationButton = true,
                enableMyLocationIndicator = true
            )
        )
        val map = this.map!!

        map.setPadding(bottom = style.summaryPeekHeight)
        map.setOnMapClickListener {
            bottomSheetOpened.postValue(false)
        }
        history.value?.let { hist ->
            displayHistory(map, hist)
            moveMap(map, hist, userLocation)
        }
    }

    fun onTileSelected(tile: HistoryTile) {
        try {
            if (tile.tileType == HistoryTileType.SUMMARY) return

            selectedSegment?.remove()
            activeMarkers.forEach { it.remove() }
            map?.googleMap?.let { googleMap ->
                selectedSegment = googleMap.addPolyline(
                    tile.locations
                        .map { LatLng(it.latitude, it.longitude) }
                        .fold(PolylineOptions()) { options, loc -> options.add(loc) }
                        .color(style.colorForStatus(tile.status))
                        .clickable(true)
                )
                tile.locations.firstOrNull()?.let {
                    activeMarkers.add(addMarker(it, googleMap, tile.address, tile.status))
                }
                tile.locations.lastOrNull()?.let {
                    activeMarkers.add(addMarker(it, googleMap, tile.address, tile.status))
                }
                //newLatLngBounds can cause crash if called before layout without map size
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        tile.locations.boundRect(),
                        style.mapPadding
                    )
                )
                googleMap.setOnMapClickListener {
                    selectedSegment?.remove()
                    activeMarkers.forEach { it.remove() }
                    activeMarkers.clear()
                    selectedSegment = null
                    viewBounds?.let { bounds ->
                        //newLatLngBounds can cause crash if called before layout without map size
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                style.mapPadding
                            )
                        )
                    }
                }
            }
            bottomSheetOpened.postValue(false)
        } catch (e: Exception) {
            errorHandler.postException(e)
        }
    }

    fun onResume() {
        historyInteractor.refreshTodayHistory()
    }

    private fun displayHistory(map: HypertrackMapWrapper, history: History) {
        errorHandler.handle {
            map.showHistory(history, style)
        }
    }

    private fun addMarker(
        location: Location,
        map: GoogleMap,
        address: CharSequence?,
        status: Status
    ): Marker {
        val markerOptions = MarkerOptions().position(LatLng(location.latitude, location.longitude))
            .icon(BitmapDescriptorFactory.fromBitmap(style.markerForStatus(status)))
        address?.let { markerOptions.title(it.toString()) }
        return map.addMarker(markerOptions)
    }

    private fun moveMap(map: HypertrackMapWrapper, history: History, userLocation: LatLng?) {
        if (!firstMovePerformed) {
            if (history.locationTimePoints.isEmpty()) {
                userLocation?.let { map.moveCamera(it) }
            } else {
                val viewBounds = history.locationTimePoints.map { it.first }.boundRect().let {
                    if (userLocation != null) {
                        it.including(userLocation)
                    } else it
                }
                //newLatLngBounds can cause crash if called before layout without map size
                try {
                    map.googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            viewBounds,
                            style.mapPadding
                        )
                    )
                } catch (e: Exception) {
                    userLocation?.let { map.moveCamera(it) }
                }
            }
            firstMovePerformed = true
        }
    }

    private fun historyToTiles(
        history: History,
        timeDistanceFormatter: TimeDistanceFormatter
    ): List<HistoryTile> {
        with(history) {
            val result = mutableListOf<HistoryTile>()
            var startMarker = true
            var ongoingStatus = Status.UNKNOWN
            for (marker in markers.sortedBy { it.timestamp }) {
                when (marker) {
                    is StatusMarker -> {
                        val tile = HistoryTile(
                            marker.status,
                            marker.asDescription(timeDistanceFormatter),
                            if (marker.status == Status.OUTAGE) {
                                mapInactiveReason(marker.reason)
                            } else {
                                marker.address
                            },
                            marker.timeFrame(timeDistanceFormatter),
                            historyTileType(startMarker, marker.status),
                            filterMarkerLocations(
                                marker.startLocationTimestamp ?: marker.startTimestamp,
                                marker.endLocationTimestamp ?: marker.endTimestamp
                                ?: marker.startTimestamp,
                                locationTimePoints
                            )
                        )
                        ongoingStatus = marker.status
                        result.add(tile)
                    }
                    is GeoTagMarker -> {
                        marker.location?.let { geotagLocation ->
                            val tile = HistoryTile(
                                ongoingStatus,
                                marker.asDescription(), null,
                                timeDistanceFormatter.formatTime(marker.timestamp),
                                historyTileType(startMarker, ongoingStatus),
                                listOf(geotagLocation), false
                            )
                            result.add(tile)
                        }
                    }
                    is GeofenceMarker -> {
                        val tile = HistoryTile(
                            ongoingStatus,
                            marker.asDescription(), null,
                            marker.asTimeFrame(timeDistanceFormatter),
                            historyTileType(startMarker, ongoingStatus),
                            filterMarkerLocations(
                                marker.arrivalTimestamp ?: marker.timestamp,
                                marker.exitTimestamp ?: marker.timestamp,
                                locationTimePoints
                            ), false
                        )
                        result.add(tile)
                    }
                }
                startMarker = result.isEmpty()

            }

            val summaryTile = HistoryTile(
                Status.UNKNOWN,
                "${formatDuration(summary.totalDuration)} • ${
                    timeDistanceFormatter.formatDistance(
                        summary.totalDistance
                    )
                }",
                null, "", HistoryTileType.SUMMARY
            )

            return result.apply { add(0, summaryTile) }
        }
    }

    private fun GeofenceMarker.asDescription(): String {
        //todo string res
        return (metadata["name"]
            ?: metadata["address"]
            ?: metadata
                ).let {
                "$it"
            }
    }

    private fun StatusMarker.asDescription(timeDistanceFormatter: TimeDistanceFormatter): String =
        when (status) {
            Status.DRIVE -> formatDriveStats(timeDistanceFormatter)
            Status.WALK -> formatWalkStats()
            else -> formatDuration(duration)
        }

    private fun StatusMarker.formatDriveStats(timeDistanceFormatter: TimeDistanceFormatter) =
        "${formatDuration(duration)} • ${timeDistanceFormatter.formatDistance(distance ?: 0)}"

    private fun StatusMarker.formatWalkStats() =
        "${formatDuration(duration)}  • ${stepsCount ?: 0} steps"

    private fun formatDuration(duration: Int) = when {
        duration / 3600 < 1 -> "${duration / 60} min"
        duration / 3600 == 1 -> "1 hour ${duration % 3600 / 60} min"
        else -> "${duration / 3600} hours ${duration % 3600 / 60} min"
    }

    private fun StatusMarker.timeFrame(timeFormatter: TimeDistanceFormatter): String {
        if (endTimestamp == null) return timeFormatter.formatTime(startTimestamp)
        return "${timeFormatter.formatTime(startTimestamp)} : ${
            timeFormatter.formatTime(
                endTimestamp
            )
        }"
    }

    private fun GeofenceMarker.asTimeFrame(formatter: TimeDistanceFormatter): String {
        val from = timestamp
        val upTo = exitTimestamp ?: timestamp
        return if (from == upTo)
            formatter.formatTime(timestamp)
        else
            "${formatter.formatTime(from)} : ${formatter.formatTime(upTo)}"
    }

    private fun historyTileType(
        startMarker: Boolean,
        status: Status
    ): HistoryTileType {
        return when {
            startMarker && status in listOf(
                Status.OUTAGE,
                Status.INACTIVE
            ) -> HistoryTileType.OUTAGE_START
            startMarker -> HistoryTileType.ACTIVE_START
            status in listOf(Status.OUTAGE, Status.INACTIVE) -> HistoryTileType.OUTAGE
            else -> HistoryTileType.ACTIVE
        }
    }

    //todo string res
    private fun GeoTagMarker.asDescription(): String = when {
        metadata.containsValue(Constants.CLOCK_IN) -> "Clock In"
        metadata.containsValue(Constants.CLOCK_OUT) -> "Clock Out"
        metadata.containsValue(Constants.PICK_UP) -> "Pick Up"
        metadata.containsValue(Constants.VISIT_MARKED_CANCELED) -> "Visit Marked Cancelled"
        metadata.containsValue(Constants.VISIT_MARKED_COMPLETE) -> "Visit Marked Complete"
        else -> "Geotag $metadata"
    }

    private fun filterMarkerLocations(
        from: String,
        upTo: String,
        locationTimePoints: List<Pair<Location, String>>
    ): List<Location> {

        check(locationTimePoints.isNotEmpty()) { "locations should not be empty for the timeline" }
        val innerLocations = locationTimePoints
            .filter { (_, time) -> time in from..upTo }
            .map { (loc, _) -> loc }
        if (innerLocations.isNotEmpty()) return innerLocations

        // Snap to adjacent
        val sorted = locationTimePoints.sortedBy { it.second }
        Log.v(TAG, "Got sorted $sorted")
        val startLocation = sorted.lastOrNull { (_, time) -> time < from }
        val endLocation = sorted.firstOrNull { (_, time) -> time > upTo }
        Log.v(TAG, "Got start $startLocation, end $endLocation")
        return listOfNotNull(startLocation?.first, endLocation?.first)

    }

    private fun mapInactiveReason(reason: String?): String? {
        return when (reason) {
            "location_permissions_denied" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_location_permissions_denied)
            }
            "location_services_disabled" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_location_services_disabled)
            }
            "motion_activity_permissions_denied" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_motion_activity_permissions_denied)
            }
            "motion_activity_services_disabled" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_motion_activity_services_disabled)
            }
            "motion_activity_services_unavailable" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_motion_activity_services_unavailable)
            }
            "tracking_stopped" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_tracking_stopped)
            }
            "tracking_service_terminated" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_tracking_service_terminated)
            }
            "unexpected" -> {
                osUtilsProvider.getString(R.string.timeline_inactive_reason_unexpected)
            }
            else -> reason
        }
    }

    companion object {
        const val TAG = "HistoryViewModel"
    }
}

class TimeDistanceFormatter(
    val datetimeFormatter: DatetimeFormatter,
    val distanceFormatter: DistanceFormatter
) {
    fun formatTime(timestamp: String): String {
        return datetimeFormatter.formatTime(datetimeFromString(timestamp))
    }

    fun formatDistance(totalDistance: Int): String {
        return distanceFormatter.formatDistance(totalDistance)
    }
}

private fun Iterable<Location>.boundRect(): LatLngBounds {
    return fold(LatLngBounds.builder()) { builder, point ->
        builder.include(point.toLatLng())
    }.build()
}