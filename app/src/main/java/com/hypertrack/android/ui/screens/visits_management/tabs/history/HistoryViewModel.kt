package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.interactors.app.HistoryViewAppAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.state.HistoryData
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import java.time.LocalDate

@Suppress("OPT_IN_USAGE")
class HistoryViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
    private val state: StateFlow<HistoryScreenState?>,
    private val dateTimeFormatter: DateTimeFormatter,
    private val timeValueFormatter: TimeValueFormatter,
    private val distanceFormatter: DistanceFormatter,
    val style: BaseHistoryStyle
) : BaseViewModel(baseDependencies) {

    val viewState = MutableLiveData<ViewState>()
    val viewEvent = MutableLiveData<Consumable<ViewEvent>>()

    init {
        runInVmEffectsScope {
            state.collect { historyScreenState ->
                viewState.postValue(getViewState(historyScreenState))
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
            handleAction(MapClickedAction)
        }
        handleAction(
            ViewReadyAction(
                map,
                viewEvent
            )
        )
    }


    fun handleAction(action: HistoryViewAction) {
        appInteractor.handleAction(HistoryViewAppAction(action))
    }

    fun onAddGeotagClick() {
        // todo to state machine
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToAddGeotagFragment().toConsumable()
        )
    }

    fun onBottomSheetStateChanged(newState: Int) {
        try {
            val state = BottomSheetState.fromInt(newState)
            when (state) {
                Expanded -> {
                    handleAction(OnBottomSheetStateChangedAction(true))
                }
                Collapsed -> {
                    handleAction(OnBottomSheetStateChangedAction(false))
                }
                Dragging, HalfExpanded, Hidden, Settling -> {
                }
            }
        } catch (exception: IllegalArgumentException) {
            showExceptionMessageAndReport(exception)
        }
    }

    fun onCopyClick(id: String) {
        // todo effect
        osUtilsProvider.copyToClipboard(id)
    }

    fun onBackPressed(): Boolean {
        val state = state.value
        return if (
            state is MapReadyState &&
            state.historyData is LoadingSuccess<HistoryData, ErrorMessage> &&
            state.historyData.data.bottomSheetExpanded
        ) {
            handleAction(OnBackPressedAction)
            true
        } else {
            false
        }
    }

    fun createTimelineAdapter(): TimelineTileItemAdapter {
        return TimelineTileItemAdapter(
            osUtilsProvider
        ) {
            handleAction(TimelineItemSelected(it))
        }
    }

    override fun onError(exception: Exception) {
        showExceptionMessageAndReport(exception)
    }

    private fun getViewState(state: HistoryScreenState?): ViewState {
        return when (state) {
            is Initial -> {
                loadingViewState()
            }
            is MapReadyState -> {
                when (state.historyData) {
                    is Loading -> loadingViewState()
                    is LoadingFailure -> {
                        ViewState(
                            dateText = null,
                            showProgressbar = false,
                            errorText = state.historyData.failure,
                            tiles = listOf(),
                            daySummaryTitle = null,
                            showTimelineRecyclerView = false,
                            showUpArrow = false,
                            showAddGeotagButton = true,
                            totalDriveDurationText = null,
                            totalDriveDistanceText = null,
                        )
                    }
                    is LoadingSuccess -> {
                        val historyData = state.historyData
                        val summary = historyData.data.summary
                        ViewState(
                            dateText = dateTimeFormatter.formatDate(state.date),
                            showProgressbar = false,
                            errorText = null,
                            tiles = historyData.data.timelineTiles,
                            showAddGeotagButton = !historyData.data.bottomSheetExpanded,
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
                            ),
                        )
                    }
                }
            }
            null -> {
                loadingViewState()
            }
        }
    }

    private fun loadingViewState(): ViewState {
        return ViewState(
            dateText = null,
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

}

