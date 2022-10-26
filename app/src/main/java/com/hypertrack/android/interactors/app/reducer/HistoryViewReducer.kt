package com.hypertrack.android.interactors.app.reducer

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppMapEffect
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.HistoryViewEffect
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.UserLocationChangedAction
import com.hypertrack.android.interactors.app.action.StartDayHistoryLoadingAction
import com.hypertrack.android.interactors.app.effect.MoveMapToBoundsEffect
import com.hypertrack.android.interactors.app.effect.MoveMapToLocationEffect
import com.hypertrack.android.interactors.app.effect.UpdateMapEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigateInGraphEffect
import com.hypertrack.android.interactors.app.state.HistorySuccessState
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.models.local.DeviceStatusMarkerActive
import com.hypertrack.android.models.local.DeviceStatusMarkerInactive
import com.hypertrack.android.models.local.History
import com.hypertrack.android.models.local.TrackingStopped
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ActiveStatusTile
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DaySummary
import com.hypertrack.android.ui.screens.visits_management.tabs.history.GeofenceVisitTile
import com.hypertrack.android.ui.screens.visits_management.tabs.history.GeotagTile
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.InactiveStatusTile
import com.hypertrack.android.ui.screens.visits_management.tabs.history.Initial
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapClickedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ViewReadyAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapReadyState
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnBackPressedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnBottomSheetStateChangedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnDateSelectedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnGeofenceClickAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnReloadPressedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnResumeAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnScrimClickAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnTimelineHeaderClickAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OpenGeofenceVisitInfoDialogEffect
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OpenGeotagInfoDialogEffect
import com.hypertrack.android.ui.screens.visits_management.tabs.history.SelectDateClickedAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.SetBottomSheetStateEvent
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ShowDatePickerDialogEvent
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimelineItemSelected
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimelineTile
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ViewEventEffect
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.R

class HistoryViewReducer(
    private val appScope: AppScope,
) {

    fun reduce(
        action: HistoryViewAction,
        viewState: HistoryScreenState,
        appHistoryState: HistoryState,
        userState: UserLoggedIn
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return withSelectedDay(appHistoryState, viewState) { historyState ->
            when (viewState) {
                is Initial -> reduce(action, viewState, historyState, userState)
                is MapReadyState -> reduce(action, viewState, historyState, userState)
            }
        }
    }

    // called on any history data change (date changed, loading finished)
    fun map(
        userState: UserLoggedIn,
        appHistoryState: HistoryState,
        viewState: HistoryScreenState
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return withSelectedDay(appHistoryState, viewState) { historyState ->
            when (viewState) {
                is Initial -> viewState.withEffects()
                is MapReadyState -> {
                    when (historyState) {
                        is LoadingSuccess -> {
                            val historyViewState = getHistorySuccessState(
                                viewState.historyState,
                                historyState.data
                            )
                            viewState.copy(
                                historyState = historyState.mapSuccess { historyViewState }
                            ).withEffects(
                                getUpdateAndMoveMapEffects(
                                    viewState.map,
                                    historyState,
                                    userState.userLocation
                                )
                            )
                        }
                        is Loading, is LoadingFailure -> {
                            viewState.copy(
                                historyState = historyState.mapSuccess {
                                    getHistorySuccessState(viewState.historyState, it)
                                }
                            ).withEffects()
                        }
                    }

                }
            }
        }
    }

    fun getEffects(
        action: UserLocationChangedAction,
        history: HistoryState,
        viewState: HistoryScreenState?
    ): Set<AppEffect> {
        return if (viewState != null &&
            viewState is MapReadyState
        ) {
            withSelectedDay(history, viewState) {
                viewState.withEffects(
                    if (it is LoadingSuccess) {
                        val mapData = getMapData(it.data, action.userLocation)
                        getMoveMapEffects(
                            viewState.map,
                            getMapData(it.data, action.userLocation)
                        ) + setOf(
                            AppMapEffect(UpdateMapEffect(viewState.map, mapData))
                        )
                    } else setOf()
                )
            }.effects
        } else setOf()
    }

    private fun reduce(
        action: HistoryViewAction,
        viewState: MapReadyState,
        historyState: LoadingState<History, ErrorMessage>,
        userState: UserLoggedIn
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return when (action) {
            is ViewReadyAction -> {
                viewState.copy(map = action.map, viewEventHandle = action.viewEventHandle)
                    .withEffects(
                        getUpdateAndMoveMapEffects(
                            action.map,
                            historyState,
                            userState.userLocation
                        ) + when (viewState.historyState) {
                            is LoadingSuccess -> {
                                changeBottomSheetStateEffect(
                                    viewState.historyState.data.bottomSheetExpanded,
                                    viewState
                                )
                            }
                            else -> setOf()
                        }
                    )
            }
            is OnDateSelectedAction -> {
                viewState.copy(date = action.date).withEffects(
                    AppActionEffect(
                        HistoryAppAction(
                            StartDayHistoryLoadingAction(
                                action.date,
                                forceReloadIfLoading = true
                            )
                        )
                    )
                )
            }
            MapClickedAction -> {
                changeBottomSheetState(action, viewState) { false }
            }
            //todo select segment
            is TimelineItemSelected -> {
                withLoadingSuccess(action, viewState) {
                    when (action.tile.payload) {
                        is GeofenceVisitTile -> {
                            viewState.withEffects(
                                HistoryViewEffect(
                                    OpenGeofenceVisitInfoDialogEffect(
                                        viewState.viewEventHandle,
                                        action.tile.payload.visit
                                    )
                                )
                            )
                        }
                        is GeotagTile -> {
                            viewState.withEffects(
                                HistoryViewEffect(
                                    OpenGeotagInfoDialogEffect(
                                        viewState.viewEventHandle,
                                        action.tile.payload.geotag
                                    )
                                )
                            )
                        }
                        is ActiveStatusTile -> viewState.withEffects()
                        is InactiveStatusTile -> viewState.withEffects()
                    }
//                                .withEffects {
//                                it.map { HistoryViewEffect(it) }.toSet()
//                            }
                }
            }
            is SelectDateClickedAction -> {
                withLoadingSuccess(action, viewState) {
                    (viewState as HistoryScreenState).withEffects(
                        HistoryViewEffect(
                            ViewEventEffect(
                                viewState.viewEventHandle,
                                ShowDatePickerDialogEvent(viewState.date)
                            )
                        )
                    )
                }
            }
            is OnBottomSheetStateChangedAction -> {
                changeBottomSheetState(action, viewState) { action.expanded }
            }
            OnBackPressedAction, OnScrimClickAction -> {
                changeBottomSheetState(action, viewState) { false }
            }
            is OnGeofenceClickAction -> {
                (viewState as HistoryScreenState).withEffects(
                    NavigateAppEffect(
                        NavigateInGraphEffect(
                            VisitsManagementFragmentDirections
                                .actionVisitManagementFragmentToPlaceDetailsFragment(action.geofenceId)
                        )
                    )
                )
            }
            OnReloadPressedAction -> {
                viewState.withEffects(
                    AppActionEffect(
                        HistoryAppAction(
                            StartDayHistoryLoadingAction(
                                viewState.date,
                                forceReloadIfTimeout = true
                            )
                        )
                    )
                )
            }
            OnResumeAction -> {
                viewState.withEffects(
                    AppActionEffect(HistoryAppAction(StartDayHistoryLoadingAction(viewState.date)))
                )
            }
            OnTimelineHeaderClickAction -> {
                withLoadingSuccess(action, viewState) {
                    if (it.timelineTiles.isNotEmpty()) {
                        changeBottomSheetState(action, viewState) { oldExpanded -> !oldExpanded }
                    } else {
                        viewState.withEffects()
                    }
                }
            }
        }
    }

    private fun reduce(
        action: HistoryViewAction,
        state: Initial,
        historyState: LoadingState<History, ErrorMessage>,
        userState: UserLoggedIn
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return when (action) {
            is ViewReadyAction -> {
                MapReadyState(
                    state.date,
                    action.map,
                    historyState.mapSuccess { getHistorySuccessState(Loading(), it) },
                    action.viewEventHandle
                ).withEffects(
                    getUpdateAndMoveMapEffects(action.map, historyState, userState.userLocation)
                )
            }
            OnResumeAction -> {
                state.withEffects()
            }
            OnBackPressedAction,
            MapClickedAction,
            is OnBottomSheetStateChangedAction,
            is OnDateSelectedAction,
            is OnGeofenceClickAction,
            OnReloadPressedAction,
            OnScrimClickAction,
            OnTimelineHeaderClickAction,
            SelectDateClickedAction,
            is TimelineItemSelected -> {
                // impossible actions
                illegalAction(action, state)
            }
        }
    }

    private fun withSelectedDay(
        appHistoryState: HistoryState,
        state: HistoryScreenState,
        block: (
            LoadingState<History, ErrorMessage>
        ) -> ReducerResult<out HistoryScreenState, out AppEffect>
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        val historyStateForThisDay = appHistoryState.days[state.selectedDay]
        return if (historyStateForThisDay != null) {
            block.invoke(historyStateForThisDay)
        } else {
            // todo error state?
            state.withEffects(
                ShowAndReportAppErrorEffect(NullPointerException(state.selectedDay.toString()))
            )
        }
    }

    private fun getHistorySuccessState(
        oldState: LoadingState<HistorySuccessState, ErrorMessage>,
        history: History
    ): HistorySuccessState {
        return HistorySuccessState(
            timelineTiles = mapHistoryToTiles(history),
            summary = DaySummary(
                history.summary.totalDriveDistance,
                history.summary.totalDriveDuration,
            ),
            bottomSheetExpanded = when (oldState) {
                is LoadingSuccess -> oldState.data.bottomSheetExpanded
                is Loading -> false
                is LoadingFailure -> false
            }
        )
    }

    private fun mapHistoryToTiles(history: History): List<TimelineTile> {
        return mutableListOf<TimelineTile>()
            .apply {
                addAll(history.visits.map {
                    TimelineTile(
                        it.arrival.value,
                        GeofenceVisitTile(it),
                        description = appScope.geofenceVisitDisplayDelegate.getGeofenceName(it),
                        timeString = appScope.geofenceVisitDisplayDelegate.getVisitTimeTextForTimeline(
                            it
                        ),
                        address = it.address,
                        locations = listOf()
                    )
                })
                addAll(history.geotags.map {
                    TimelineTile(
                        it.createdAt,
                        GeotagTile(it),
                        description = appScope.osUtilsProvider.stringFromResource(R.string.geotag),
                        address = appScope.geotagDisplayDelegate.formatMetadata(it),
                        timeString = appScope.geotagDisplayDelegate.getTimeTextForTimeline(it),
                        locations = listOf(),
                    )
                })
                addAll(history.deviceStatusMarkers.map {
                    TimelineTile(
                        it.ongoingStatus.getDateTimeRange().start.value,
                        when (it) {
                            is DeviceStatusMarkerActive -> ActiveStatusTile(it.activity)
                            is DeviceStatusMarkerInactive -> InactiveStatusTile(it.reason)
                        },
                        isStart = false,
                        description = appScope.deviceStatusMarkerDisplayDelegate.getDescription(it),
                        address = appScope.deviceStatusMarkerDisplayDelegate.getAddress(it),
                        timeString = appScope.deviceStatusMarkerDisplayDelegate.getTimeStringForTimeline(
                            it
                        ),
                        locations = listOf()
                    )
                })
            }
            .sortedBy { tile -> tile.datetime }
            .toMutableList()
            .apply {
                firstOrNull()?.let { this[0] = this[0].copy(isStart = true) }
            }
            .let {
                it.mapIndexed { index, timelineTile ->
                    if (index > 0) {
                        val previous = it[index - 1]
                        if (
                            previous.payload is InactiveStatusTile &&
                            previous.payload.outageReason == TrackingStopped
                        ) {
                            timelineTile.copy(isStart = true)
                        } else timelineTile
                    } else timelineTile
                }
            }
    }

    private fun illegalAction(
        action: HistoryViewAction,
        state: HistoryScreenState
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        val exception = IllegalActionException(action, state)
        return if (MyApplication.DEBUG_MODE) {
            throw exception
        } else {
            state.withEffects(ShowAndReportAppErrorEffect(exception))
        }
    }

    private fun withLoadingSuccess(
        action: HistoryViewAction,
        state: MapReadyState,
        block: (HistorySuccessState) -> ReducerResult<out HistoryScreenState, out AppEffect>
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return when (state.historyState) {
            is LoadingSuccess -> block.invoke(state.historyState.data)
            is Loading, is LoadingFailure -> illegalAction(action, state)
        }
    }

    private fun changeBottomSheetState(
        action: HistoryViewAction,
        state: MapReadyState,
        toExpandedState: (Boolean) -> Boolean
    ): ReducerResult<out HistoryScreenState, out AppEffect> {
        return withLoadingSuccess(action, state) { historyData ->
            val newState = toExpandedState.invoke(historyData.bottomSheetExpanded)
            state.copy(historyState = state.historyState.mapSuccess {
                it.copy(bottomSheetExpanded = newState)
            }).withEffects(
                if (historyData.bottomSheetExpanded != newState) {
                    changeBottomSheetStateEffect(newState, state)
                } else setOf()
            )
        }
    }

    private fun changeBottomSheetStateEffect(
        newState: Boolean,
        state: MapReadyState
    ): Set<AppEffect> {
        return setOf(
            HistoryViewEffect(
                ViewEventEffect(
                    state.viewEventHandle, SetBottomSheetStateEvent(
                        expanded = newState,
                        arrowDown = newState,
                    )
                )
            )
        )
    }

    private fun getUpdateAndMoveMapEffects(
        map: HypertrackMapWrapper,
        history: LoadingState<History, ErrorMessage>,
        userLocation: LatLng?
    ): Set<AppEffect> {
        return when (history) {
            is LoadingSuccess -> {
                val mapData = getMapData(history.data, userLocation)
                setOf(
                    AppMapEffect(UpdateMapEffect(map, mapData)),
                ) + getMoveMapEffects(map, mapData)
            }
            is Loading, is LoadingFailure -> setOf()
        }
    }

    private fun getMapData(history: History, userLocation: LatLng?): MapData {
        return MapData(
            userLocation,
            history.visits.mapNotNull {
                it.location?.let(appScope.mapItemsFactory::createGeofenceVisitMarker)
            },
            history.geotags.map {
                appScope.mapItemsFactory.createGeotagMarker(it.location)
            },
            appScope.mapItemsFactory.createHistoryPolyline(history.locations),
        )
    }

    // todo move getting bounds to effects
    private fun getMoveMapEffects(map: HypertrackMapWrapper, mapData: MapData): Set<AppEffect> {
        return try {
            mapData.historyPolyline.polylineOptions.points.let { polyline ->
                if (polyline.size > 1) {
                    LatLngBounds.builder().let { builder ->
                        polyline.forEach {
                            builder.include(it)
                        }
                        mapData.userLocation?.let {
                            builder.include(it)
                        }
                        setOf(AppMapEffect(MoveMapToBoundsEffect(map, builder.build())))
                    }
                } else {
                    getMoveToUserLocationEffect(map, mapData.userLocation)
                }
            }
        } catch (e: Exception) {
            setOf(
                ShowAndReportAppErrorEffect(e)
            ) + getMoveToUserLocationEffect(map, mapData.userLocation)
        }
    }

    private fun getMoveToUserLocationEffect(
        map: HypertrackMapWrapper,
        userLocation: LatLng?
    ): Set<AppEffect> {
        return userLocation?.let {
            setOf(AppMapEffect(MoveMapToLocationEffect(map, it)))
        } ?: setOf()
    }


}
