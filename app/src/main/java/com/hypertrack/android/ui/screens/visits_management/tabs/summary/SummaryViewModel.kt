package com.hypertrack.android.ui.screens.visits_management.tabs.summary

import androidx.lifecycle.*
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.action.RefreshSummaryAction
import com.hypertrack.android.models.local.History
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter

import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class SummaryViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val state: StateFlow<LoadingState<History, ErrorMessage>?>,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeValueFormatter,
) : BaseViewModel(baseDependencies) {

    val viewState = MutableLiveData<ViewState>()

    init {
        runInVmEffectsScope {
            state.collect {
                it?.let { historyState ->
                    // viewState is null in some cases
                    @Suppress(
                        "SAFE_CALL_WILL_CHANGE_NULLABILITY",
                        "UNNECESSARY_SAFE_CALL",
                        "USELESS_ELVIS"
                    )
                    viewState?.postValue(getViewState(historyState)) ?: run {
                        crashReportsProvider.logException(NullPointerException("viewState"))
                    }
                }
            }
        }
    }

    private fun getViewState(historyState: LoadingState<History, ErrorMessage>): ViewState {
        return when (historyState) {
            is LoadingSuccess -> {
                ViewState(getSummary(historyState.data), isLoading = false)
            }
            is LoadingFailure, is Loading -> {
                // todo error state
                ViewState(listOf(), isLoading = true)
            }
        }
    }

    private fun getSummary(history: History): List<SummaryItem> {
        return history.summary.let { summary ->
            listOf(
                //for some reason there is no total duration in GraphQl API
//                SummaryItem(
//                    R.drawable.ic_ht_eta,
//                    osUtilsProvider.stringFromResource(R.string.summary_total_tracking_time),
//                    timeFormatter.formatTimeValue(summary.totalDuration)
//                ),
                SummaryItem(
                    R.drawable.ic_ht_drive,
                    osUtilsProvider.stringFromResource(R.string.summary_drive),
                    timeFormatter.formatTimeValue(summary.totalDriveDuration),
                    distanceFormatter.formatDistance(summary.totalDriveDistance)
                ),
                SummaryItem(
                    R.drawable.ic_ht_walk,
                    osUtilsProvider.stringFromResource(R.string.summary_walk),
                    timeFormatter.formatTimeValue(summary.totalWalkDuration),
                    osUtilsProvider.stringFromResource(R.string.steps, summary.stepsCount)
                ),
                SummaryItem(
                    R.drawable.ic_ht_stop,
                    osUtilsProvider.stringFromResource(R.string.summary_stop),
                    timeFormatter.formatTimeValue(summary.totalStopDuration)
                ),
            )
        }
    }

    fun refreshSummary() {
        appInteractor.handleAction(HistoryAppAction(RefreshSummaryAction))
    }
}
