package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.hypertrack.android.utils.ErrorMessage
import java.time.LocalDate

data class ViewState(
    val dateText: String?,
    val showProgressbar: Boolean,
    val errorText: ErrorMessage?,
    val showTimelineRecyclerView: Boolean,
    val showUpArrow: Boolean,
    val showAddGeotagButton: Boolean,
    val tiles: List<TimelineTile>,
    val totalDriveDistanceText: String?,
    val totalDriveDurationText: String?,
    val daySummaryTitle: String?,
)
