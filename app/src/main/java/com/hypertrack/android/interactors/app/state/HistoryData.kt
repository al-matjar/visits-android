package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.ui.screens.visits_management.tabs.history.DaySummary
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimelineTile
import com.hypertrack.android.utils.MyApplication

data class HistoryData(
    val timelineTiles: List<TimelineTile>,
    val summary: DaySummary,
    val bottomSheetExpanded: Boolean
) {
    override fun toString(): String {
        return if (MyApplication.DEBUG_MODE) {
            "${javaClass.simpleName}(timelineTiles=${timelineTiles.size}), summary=${summary.javaClass.simpleName}, bottomSheetExpanded=$bottomSheetExpanded"
        } else {
            super.toString()
        }

    }
}
