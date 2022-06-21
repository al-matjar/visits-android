package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.datetime.TimeValue

data class DaySummary(
    val totalDriveDistance: DistanceValue,
    val totalDriveDuration: TimeValue
) {
    fun isZero() = totalDriveDistance.meters == 0 && totalDriveDuration.totalSeconds == 0L
}
