package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.ui.screens.visits_management.tabs.history.DaySummary
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimelineTile
import com.hypertrack.android.utils.MyApplication

data class HistorySuccessState(
    val timelineTiles: List<TimelineTile>,
    val summary: DaySummary,
    val bottomSheetExpanded: Boolean
) {
    override fun toString(): String {
        return "${javaClass.simpleName}(timelineTiles=${timelineTiles.size}), summary=${summary.javaClass.simpleName}, bottomSheetExpanded=$bottomSheetExpanded"
    }
}
