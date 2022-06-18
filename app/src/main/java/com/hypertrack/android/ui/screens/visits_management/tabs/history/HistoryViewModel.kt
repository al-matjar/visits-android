package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.interactors.history.HistoryState
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.DeviceStatusMarkerActive
import com.hypertrack.android.models.local.DeviceStatusMarkerInactive
import com.hypertrack.android.models.local.LocalHistory
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.delegates.DeviceStatusMarkerDisplayDelegate
import com.hypertrack.android.ui.common.delegates.display.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.delegates.display.GeotagDisplayDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceVisitAddressDelegate
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.util.LocationUtils
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter

import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HistoryViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
    private val geocodingInteractor: GeocodingInteractor,
    private val geofenceVisitAddressDelegate: GeofenceVisitAddressDelegate,
    private val visitDisplayDelegate: GeofenceVisitDisplayDelegate,
    private val statusMarkerDisplayDelegate: DeviceStatusMarkerDisplayDelegate,
    private val geotagDisplayDelegate: GeotagDisplayDelegate,
    private val timeValueFormatter: TimeValueFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val mapItemsFactory: HypertrackMapItemsFactory,
    val style: BaseHistoryStyle
) : BaseViewModel(baseDependencies) {

    val timelineAdapter = TimelineTileItemAdapter(
        osUtilsProvider
    ) { onTileSelected(it) }

    val errorTextState = MutableLiveData<ErrorMessage?>()
    val daySummaryTexts = MutableLiveData<SummaryTexts>()
    val currentDateText = MutableLiveData<String?>()
    val showTimelineArrow = MutableLiveData<Boolean>()
    val timelineArrowDirectionDown = MutableLiveData<Boolean>()
    val showAddGeotagButton = MutableLiveData<Boolean>()
    val openDatePickerDialogEvent = MutableLiveData<Consumable<LocalDate>>()
    val setBottomSheetExpandedEvent = MutableLiveData<Boolean>()
    val openDialogEvent = MutableLiveData<Consumable<TimelineDialog>>()

    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        Initial(LocalDate.now(), Loading(), null),
        viewModelScope,
        Dispatchers.Main,
        this::handleAction,
        this::applyEffects,
        this::stateChangeEffects
    )

    init {
        appInteractor.appState.observeManaged { state ->
            when (state) {
                is Initialized -> {
                    when (state.userState) {
                        is UserLoggedIn -> {
                            val userScope = state.userState.userScope
                            userScope.historyInteractor.history.observeManaged {
                                stateMachine.handleAction(HistoryUpdatedAction(it))
                            }

                            userScope.deviceLocationProvider.getCurrentLocation { location ->
                                location?.let { latLng ->
                                    stateMachine.handleAction(UserLocationReceived(latLng))
                                }
                            }
                        }
                        UserNotLoggedIn -> {
                            crashReportsProvider.logException(IllegalStateException(state.toString()))
                        }
                    }
                }
                is NotInitialized -> {
                    crashReportsProvider.logException(IllegalStateException(state.toString()))
                }
            }
        }

    }

    private fun handleAction(state: State, action: Action): ReducerResult<State, Effect> {
        val appState = appInteractor.appState.requireValue()
        return when (appState) {
            is Initialized -> {
                when (appState.userState) {
                    is UserLoggedIn -> {
                        handleActionIfLoggedIn(state, action, appState.userState.userScope)
                    }
                    UserNotLoggedIn -> {
                        state.withEffects(
                            SendErrorToCrashlytics(
                                IllegalActionException(action, state)
                            )
                        )
                    }
                }
            }
            is NotInitialized -> {
                state.withEffects(
                    SendErrorToCrashlytics(
                        IllegalActionException(action, state)
                    )
                )
            }
        }
    }

    private fun handleActionIfLoggedIn(
        state: State,
        action: Action,
        userScope: UserScope
    ): ReducerResult<State, Effect> {
        return when (action) {
            is MapReadyAction -> {
                when (state) {
                    is Initial -> {
                        MapReadyState(
                            state.date,
                            action.map,
                            state.historyData,
                            state.userLocation,
                            false,
                        ).withEffects(
                            getMoveMapEffectIfNeeded(
                                action.map,
                                state.historyData,
                                state.userLocation
                            )
                        )
                    }
                    is MapReadyState -> {
                        //todo re-add map data?
                        state.copy(map = action.map).asReducerResult()
                    }
                }
            }
            is HistoryUpdatedAction -> {
                //todo handle selection inheritance
                when (state) {
                    is Initial -> {
                        state.copy(historyData = getTodayHistory(state.date, action.history).map {
                            mapHistoryToViewData(
                                it,
                                NotSelected
                            )
                        }).asReducerResult()
                    }
                    is MapReadyState -> {
                        val historyData = getTodayHistory(state.date, action.history).map {
                            mapHistoryToViewData(
                                it,
                                NotSelected
                            )
                        }
                        state.copy(historyData = historyData).withEffects(
                            getMoveMapEffectIfNeeded(
                                state.map,
                                historyData,
                                state.userLocation
                            )
                        )
                    }
                }
            }
            is UserLocationReceived -> {
                when (state) {
                    is Initial -> state.copy(userLocation = action.userLocation).asReducerResult()
                    is MapReadyState -> {
                        state.copy(userLocation = action.userLocation)
                            .withEffects(
                                getMoveMapEffectIfNeeded(
                                    state.map,
                                    state.historyData,
                                    action.userLocation
                                )
                            )
                    }
                }
            }
            is MapClickedAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        when (state.historyData) {
                            is LoadingSuccess -> {
                                when (state.historyData.data.mapData.segmentSelection) {
                                    is NotSelected -> {
                                        state.withEffects(SetBottomSheetStateEffect(expanded = false))
                                    }
                                    is SelectedSegment -> {
                                        withSegmentSelection(state, state.historyData, NotSelected)
                                            .asReducerResult()
                                        //todo move map to whole history
                                    }
                                }
                            }
                            is Loading, is LoadingFailure -> {
                                state.withEffects(IllegalActionEffect(action, state))
                            }
                        }

                    }
                }

            }
            is TimelineItemSelected -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        when (state.historyData) {
                            is LoadingSuccess -> {
                                when (action.tile.payload) {
                                    is GeofenceVisitTile -> state.withEffects(
                                        OpenGeofenceVisitInfoDialogEffect(action.tile.payload.visit)
                                    )
                                    is GeotagTile -> state.withEffects(
                                        OpenGeotagInfoDialogEffect(action.tile.payload.geotag)
                                    )
                                    is ActiveStatusTile -> state.asReducerResult()
                                    is InactiveStatusTile -> state.asReducerResult()
                                }
                                //todo selection
//                                withSegmentSelection(state, state.historyData, SelectedSegment(
//                                    action.tile.locations
//                                        .fold(PolylineOptions()) { options, loc ->
//                                            options.add(
//                                                loc
//                                            )
//                                        }
//                                        .color(style.colorForStatus(action.tile.status))
//                                        .clickable(true),
//                                    getEdgeMarkers(action.tile)
//                                )).withEffects(
//                                    CloseBottomSheetEffect,
//                                    MoveMapToBoundsEffect(
//                                        state.map,
//                                        action.tile.locations.boundRect(),
//                                        style.mapPadding
//                                    )
//                                )
                            }
                            is Loading, is LoadingFailure -> {
                                state.withEffects(IllegalActionEffect(action, state))
                            }
                        }
                    }
                }
            }
            SelectDateClickedAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        state.withEffects(ShowDatePickerDialogEffect(state.date))
                    }
                }
            }
            is OnDateSelectedAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        state.copy(date = action.date, historyData = Loading())
                            .withEffects(LoadHistoryEffect(userScope, action.date))
                    }
                }
            }
            is OnBottomSheetStateChangedAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        state.copy(bottomSheetExpanded = action.expanded).asReducerResult()
                    }
                }
            }
            OnBackPressedAction -> {
                state.withEffects(SetBottomSheetStateEffect(expanded = false))
            }
            is OnGeofenceClickAction -> {
                state.withEffects(OpenGeofenceDetailsEffect(action.geofenceId))
            }
            is OnErrorAction -> {
                when (state) {
                    is Initial -> {
                        state.copy(historyData = LoadingFailure(action.exception))
                            .withEffects(SendErrorToCrashlytics(action.exception))
                    }
                    is MapReadyState -> {
                        state.copy(historyData = LoadingFailure(action.exception))
                            .withEffects(SendErrorToCrashlytics(action.exception))
                    }
                }
            }
            OnReloadPressedAction, OnResumeAction -> {
                when (state) {
                    is Initial -> state.withEffects(LoadHistoryEffect(userScope, state.date))
                    is MapReadyState -> state.withEffects(LoadHistoryEffect(userScope, state.date))
                }
            }
            OnTimelineHeaderClickAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        when (state.historyData) {
                            is Loading, is LoadingFailure -> {
                                state.asReducerResult()
                            }
                            is LoadingSuccess -> {
                                if (state.historyData.data.timelineTiles.isNotEmpty()) {
                                    state.withEffects(
                                        SetBottomSheetStateEffect(
                                            expanded = !state.bottomSheetExpanded
                                        )
                                    )
                                } else {
                                    state.asReducerResult()
                                }
                            }
                        }
                    }
                }
            }
            OnScrimClickAction -> {
                when (state) {
                    is Initial -> {
                        state.withEffects(IllegalActionEffect(action, state))
                    }
                    is MapReadyState -> {
                        state.withEffects(SetBottomSheetStateEffect(expanded = false))
                    }
                }
            }
        }
    }

    private fun applyEffects(effects: Set<Effect>) {
        try {
            viewModelScope.launch {
                effects.forEach { effect ->
                    handleEffect(effect)
                }
            }
        } catch (e: Exception) {
            stateMachine.handleAction(OnErrorAction(e))
        }
    }

    private suspend fun handleEffect(effect: Effect) {
        when (effect) {
            is MoveMapEffect -> {
                when (effect.target) {
                    is Either.Left -> effect.map.animateCamera(effect.target.left)
                    is Either.Right -> effect.map.animateCameraToBounds(effect.target.right)
                } as Any?
            }
            is SetBottomSheetStateEffect -> {
                setBottomSheetExpandedEvent.postValue(effect.expanded)
                timelineArrowDirectionDown.postValue(effect.expanded)
            }
            is MoveMapToBoundsEffect -> {
                effect.map.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        effect.latLngBounds,
                        effect.mapPadding
                    )
                )
            }
            is UpdateMapEffect -> {
                effect.map.clear()
                effect.userLocation?.let {
                    effect.map.addUserLocation(it)
                }
                effect.map.addPolyline(effect.mapHistoryData.historyPolyline.polylineOptions)
                effect.mapHistoryData.geotags.forEach { effect.map.addMarker(it.markerOptions) }
                effect.mapHistoryData.geofenceVisits.forEach { effect.map.addMarker(it.markerOptions) }
            }
            is UpdateViewStateEffect -> {
                displayViewState(effect.viewState)
            }
            is ShowDatePickerDialogEffect -> {
                openDatePickerDialogEvent.postValue(effect.date)
            }
            is LoadHistoryEffect -> {
                effect.userScope.historyInteractor.loadHistory(effect.date)
            }
            is SendErrorToCrashlytics -> {
                crashReportsProvider.logException(effect.exception)
            }
            is OpenGeofenceVisitInfoDialogEffect -> {
                effect.visit.id?.let {
                    openDialogEvent.postValue(
                        GeofenceVisitDialog(
                            visitId = effect.visit.id,
                            geofenceId = effect.visit.geofenceId,
                            geofenceName = visitDisplayDelegate.getGeofenceName(effect.visit),
                            geofenceDescription = visitDisplayDelegate.getGeofenceDescription(
                                effect.visit
                            ),
                            integrationName = effect.visit.metadata?.integration?.name,
                            address = geofenceVisitAddressDelegate.shortAddress(effect.visit),
                            durationText = visitDisplayDelegate.getDurationText(effect.visit),
                            routeToText = visitDisplayDelegate.getRouteToText(effect.visit)
                        )
                    )
                }
            }
            is OpenGeotagInfoDialogEffect -> {
                openDialogEvent.postValue(
                    GeotagDialog(
                        geotagId = effect.geotag.id,
                        title = geotagDisplayDelegate.getDescription(effect.geotag),
                        metadataString = geotagDisplayDelegate.formatMetadata(effect.geotag),
                        routeToText = geotagDisplayDelegate.getRouteToText(effect.geotag),
                        address = effect.geotag.address
                            ?: geocodingInteractor.getPlaceFromCoordinates(effect.geotag.location)
                                ?.toAddressString(strictMode = false)
                    )
                )
            }
            is OpenGeofenceDetailsEffect -> {
                destination.postValue(
                    VisitsManagementFragmentDirections
                        .actionVisitManagementFragmentToPlaceDetailsFragment(effect.geofenceId)
                )
            }
            is IllegalActionEffect -> {
                crashReportsProvider.logException(
                    IllegalActionException(
                        effect.action, effect.state
                    )
                )
            }
        } as Any?
    }

    private fun stateChangeEffects(newState: State): Set<Effect> {
        return when (newState) {
            is Initial -> setOf(
                UpdateViewStateEffect(
                    getViewStateForHistory(
                        newState.date,
                        true,
                        newState.historyData
                    )
                )
            )
            is MapReadyState -> {
                setOf(
                    UpdateViewStateEffect(
                        getViewStateForHistory(
                            newState.date,
                            !newState.bottomSheetExpanded,
                            newState.historyData
                        )
                    ),
                ) + getUpdateMapEffectIfNeeded(newState)
            }
        }
    }

    fun onMapReady(googleMap: GoogleMap) {
        val map = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = false,
                enableMyLocationButton = false,
                enableMyLocationIndicator = false
            )
        )
        map.setPadding(bottom = style.summaryPeekHeight)
        map.setOnMapClickListener {
            stateMachine.handleAction(MapClickedAction)
        }
        stateMachine.handleAction(MapReadyAction(map))
    }

    private fun onTileSelected(tile: TimelineTile) {
        stateMachine.handleAction(TimelineItemSelected(tile))
    }

    fun onResume() {
        stateMachine.handleAction(OnResumeAction)
    }

    private fun getUpdateMapEffectIfNeeded(newState: MapReadyState): Set<Effect> {
        return when (newState.historyData) {
            is LoadingSuccess -> setOf(
                newState.historyData.data.mapData.let {
                    UpdateMapEffect(newState.map, newState.userLocation, it)
                }
            )
            is Loading, is LoadingFailure -> setOf()
        }
    }

    private fun createOptionsForEdgeMarker(
        latLng: LatLng,
        address: String?,
        status: Status
    ): MarkerOptions {
        return MarkerOptions().position(LatLng(latLng.latitude, latLng.longitude))
            .icon(BitmapDescriptorFactory.fromBitmap(style.markerForStatus(status)))
            .also { markerOptions ->
                address?.let { markerOptions.title(it) }
            }
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
        val startLocation = sorted.lastOrNull { (_, time) -> time < from }
        val endLocation = sorted.firstOrNull { (_, time) -> time > upTo }
        return listOfNotNull(startLocation?.first, endLocation?.first)
    }

    fun onAddGeotagClick() {
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToAddGeotagFragment().toConsumable()
        )
    }

    private fun getViewStateForHistory(
        date: LocalDate,
        showAddGeotagButton: Boolean,
        historyData: LoadingState<HistoryData>
    ): ViewState {
        return when (historyData) {
            is LoadingSuccess -> {
                val summary = historyData.data.summary
                ViewState(
                    date = date,
                    showProgressbar = false,
                    errorText = null,
                    tiles = historyData.data.timelineTiles,
                    showAddGeotagButton = showAddGeotagButton,
                    showTimelineRecyclerView = !summary.isZero(),
                    showUpArrow = historyData.data.timelineTiles.isNotEmpty(),
                    totalDriveDurationText = if (!summary.isZero()) {
                        timeValueFormatter.formatTimeValue(summary.totalDriveDuration).let {
                            osUtilsProvider.stringFromResource(
                                R.string.timeline_summary_template_duration,
                                it
                            )
                        }
                    } else null,
                    totalDriveDistanceText = if (!summary.isZero()) {
                        distanceFormatter.formatDistance(summary.totalDriveDistance).let {
                            osUtilsProvider.stringFromResource(
                                R.string.timeline_summary_template_distance,
                                it
                            )
                        }
                    } else null,
                    daySummaryTitle = osUtilsProvider.stringFromResource(
                        if (!summary.isZero()) {
                            R.string.timeline_summary
                        } else {
                            R.string.timeline_empty_summary
                        }
                    )
                )
            }
            is Loading -> {
                ViewState(
                    date = null,
                    showProgressbar = true,
                    errorText = null,
                    tiles = listOf(),
                    daySummaryTitle = null,
                    showTimelineRecyclerView = false,
                    showUpArrow = false,
                    showAddGeotagButton = true,
                    totalDriveDurationText = null,
                    totalDriveDistanceText = null,
                )
            }
            is LoadingFailure -> {
                ViewState(
                    date = null,
                    showProgressbar = false,
                    errorText = osUtilsProvider.getErrorMessage(historyData.exception),
                    tiles = listOf(),
                    daySummaryTitle = null,
                    showTimelineRecyclerView = false,
                    showUpArrow = false,
                    showAddGeotagButton = true,
                    totalDriveDurationText = null,
                    totalDriveDistanceText = null,
                )
            }
        }
    }

    private fun displayViewState(viewState: ViewState) {
        loadingState.postValue(viewState.showProgressbar)
        errorTextState.postValue(viewState.errorText)
        currentDateText.postValue(
            viewState.date?.format(
                DateTimeFormatter.ofLocalizedDate(
                    FormatStyle.MEDIUM
                )
            )
        )
        timelineAdapter.updateItems(viewState.tiles)
        daySummaryTexts.postValue(
            SummaryTexts(
                title = viewState.daySummaryTitle,
                totalDriveDistance = viewState.totalDriveDistanceText,
                totalDriveDuration = viewState.totalDriveDurationText,
            )
        )
        showTimelineArrow.postValue(viewState.showUpArrow)
        showAddGeotagButton.postValue(viewState.showAddGeotagButton)
    }

    private fun mapHistoryToTiles(localHistory: LocalHistory): List<TimelineTile> {
        return mutableListOf<TimelineTile>()
            .apply {
                addAll(localHistory.visits.map {
                    TimelineTile(
                        it.arrival.value,
                        GeofenceVisitTile(it),
                        isStart = false,
                        isOutage = false,
                        description = visitDisplayDelegate.getGeofenceName(it),
                        timeString = visitDisplayDelegate.getVisitTimeTextForTimeline(it),
                        address = it.address,
                        locations = listOf()
                    )
                })
                addAll(localHistory.geotags.map {
                    TimelineTile(
                        it.createdAt,
                        GeotagTile(it),
                        isStart = false,
                        isOutage = false,
                        description = osUtilsProvider.stringFromResource(R.string.geotag),
                        address = geotagDisplayDelegate.formatMetadata(it),
                        timeString = geotagDisplayDelegate.getTimeTextForTimeline(it),
                        locations = listOf(),
                    )
                })
                addAll(localHistory.deviceStatusMarkers.map {
                    TimelineTile(
                        it.ongoingStatus.getDateTimeRange().start.value,
                        when (it) {
                            is DeviceStatusMarkerActive -> ActiveStatusTile(it.activity)
                            is DeviceStatusMarkerInactive -> InactiveStatusTile(it.reason)
                        },
                        isStart = false,
                        isOutage = false,
                        description = statusMarkerDisplayDelegate.getDescription(it),
                        address = statusMarkerDisplayDelegate.getAddress(it),
                        timeString = statusMarkerDisplayDelegate.getTimeStringForTimeline(it),
                        locations = listOf()
                    )
                })
            }
            .sortedBy { tile -> tile.datetime }
            .toMutableList()
            .apply {
                firstOrNull()?.let { this[0] = this[0].copy(isStart = true) }
            }
    }

    private fun mapHistoryToViewData(
        localHistory: LocalHistory,
        selection: SegmentSelection
    ): HistoryData {
        return HistoryData(
            MapHistoryData(
                localHistory.visits.mapNotNull {
                    it.location?.let(mapItemsFactory::createGeofenceVisitMarker)
                },
                localHistory.geotags.map {
                    mapItemsFactory.createGeotagMarker(it.location)
                },
                mapItemsFactory.createHistoryPolyline(localHistory.locations),
                selection
            ),
            mapHistoryToTiles(localHistory),
            DaySummary(
                localHistory.totalDriveDistance,
                localHistory.totalDriveDuration,
            )
        )
    }

    private fun getTodayHistory(
        date: LocalDate,
        history: HistoryState
    ): LoadingState<LocalHistory> {
        return history.days[date] ?: LoadingFailure(
            IllegalStateException("Today history is null")
        )
    }

//    private fun getEdgeMarkers(tile: TimelineTile): List<MarkerOptions> {
//        return mutableListOf<MarkerOptions>().also { markers ->
//            tile.locations.firstOrNull()?.let {
//                createOptionsForEdgeMarker(
//                    it.toLatLng(),
//                    tile.address.toString(),
//                    tile.status
//                ).let { marker ->
//                    markers.add(marker)
//                }
//            }
//            tile.locations.lastOrNull()?.let {
//                createOptionsForEdgeMarker(
//                    it.toLatLng(),
//                    tile.address.toString(),
//                    tile.status
//                ).let { marker ->
//                    markers.add(marker)
//                }
//            }
//        }
//    }

    fun onSelectDateClick() {
        stateMachine.handleAction(SelectDateClickedAction)
    }

    fun onDateSelected(date: LocalDate) {
        stateMachine.handleAction(OnDateSelectedAction(date))
    }

    private fun getMoveMapEffectIfNeeded(
        map: HypertrackMapWrapper,
        historyData: LoadingState<HistoryData>,
        userLocation: LatLng?
    ): Set<Effect> {
        val history = when (historyData) {
            is Loading, is LoadingFailure -> null
            is LoadingSuccess -> historyData.data
        }

        val historyPoints = history?.mapData?.historyPolyline?.polylineOptions?.points
        return if (!historyPoints.isNullOrEmpty()) {
            try {
                LatLngBounds.builder().apply {
                    historyPoints.forEach {
                        include(it)
                    }
                    userLocation?.let { include(it) }
                }.build().let {
                    if ((LocationUtils.distanceMeters(it.northeast, it.southwest) ?: 0)
                        > ZOOM_RADIUS_THRESHOLD.meters
                    ) {
                        setOf(MoveMapEffect(map, Either.Right(it)))
                    } else {
                        setOf(MoveMapEffect(map, Either.Left(it.center)))
                    }
                }
            } catch (e: Exception) {
                (userLocation?.let { setOf(MoveMapEffect(map, Either.Left(it))) } ?: setOf()) +
                        setOf(SendErrorToCrashlytics(e))
            }
        } else {
            userLocation?.let { setOf(MoveMapEffect(map, Either.Left(it))) } ?: setOf()
        }
    }

    fun onBottomSheetStateChanged(newState: Int) {
        try {
            val state = BottomSheetState.fromInt(newState)
            when (state) {
                Expanded -> {
                    stateMachine.handleAction(OnBottomSheetStateChangedAction(true))
                }
                Collapsed -> {
                    stateMachine.handleAction(OnBottomSheetStateChangedAction(false))
                }
                Dragging, HalfExpanded, Hidden, Settling -> {
                }
            }
        } catch (e: IllegalArgumentException) {
            stateMachine.handleAction(OnErrorAction(e))
        }
    }

    fun onCopyClick(id: String) {
        osUtilsProvider.copyToClipboard(id)
    }

    fun onGeofenceClick(geofenceId: String) {
        stateMachine.handleAction(OnGeofenceClickAction(geofenceId))
    }

    fun onBackPressed(): Boolean {
        return when (val state = stateMachine.state) {
            is Initial -> false
            is MapReadyState -> if (state.bottomSheetExpanded) {
                stateMachine.handleAction(OnBackPressedAction)
                true
            } else {
                false
            }
        }
    }

    fun onError(e: Exception) {
        stateMachine.handleAction(OnErrorAction(e))
    }

    fun onReloadClicked() {
        stateMachine.handleAction(OnReloadPressedAction)
    }

    fun onScrimClick() {
        stateMachine.handleAction(OnScrimClickAction)
    }

    fun onTimelineHeaderClick() {
        stateMachine.handleAction(OnTimelineHeaderClickAction)
    }

    companion object {
        val ZOOM_RADIUS_THRESHOLD = 500.toMeters()
    }
}

data class SummaryTexts(
    val title: String?,
    val totalDriveDistance: String?,
    val totalDriveDuration: String?,
)

fun State.asReducerResult(): ReducerResult<State, Effect> {
    return ReducerResult(this)
}

fun State.withEffects(effects: Set<Effect>): ReducerResult<State, Effect> {
    return ReducerResult(this, effects)
}

fun State.withEffects(vararg effect: Effect): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}
