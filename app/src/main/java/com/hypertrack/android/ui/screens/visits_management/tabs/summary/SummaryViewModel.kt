package com.hypertrack.android.ui.screens.visits_management.tabs.summary

import androidx.lifecycle.*
import com.hypertrack.android.interactors.HistoryInteractor
import com.hypertrack.android.models.HistoryError
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter

import com.hypertrack.logistics.android.github.R

class SummaryViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val historyInteractor: HistoryInteractor,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeFormatter,
) : BaseViewModel(baseDependencies) {

    val summary: LiveData<List<SummaryItem>> = Transformations.map(historyInteractor.todayHistory) {
        it?.summary?.let { summary ->
            listOf(
                SummaryItem(
                    R.drawable.ic_ht_eta,
                    osUtilsProvider.stringFromResource(R.string.summary_total_tracking_time),
                    timeFormatter.formatSeconds(summary.totalDuration)
                ),
                SummaryItem(
                    R.drawable.ic_ht_drive,
                    osUtilsProvider.stringFromResource(R.string.summary_drive),
                    timeFormatter.formatSeconds(summary.totalDriveDuration),
                    distanceFormatter.formatDistance(summary.totalDriveDistance)
                ),
                SummaryItem(
                    R.drawable.ic_ht_walk,
                    osUtilsProvider.stringFromResource(R.string.summary_walk),
                    timeFormatter.formatSeconds(summary.totalWalkDuration),
                    osUtilsProvider.stringFromResource(R.string.steps, summary.stepsCount)
                ),
                SummaryItem(
                    R.drawable.ic_ht_stop,
                    osUtilsProvider.stringFromResource(R.string.summary_stop),
                    timeFormatter.formatSeconds(summary.totalStopDuration)
                ),
            )
        }
    }

    val error = MutableLiveData<HistoryError>()

    fun refreshSummary() {
        historyInteractor.refreshTodayHistory()
    }
}
