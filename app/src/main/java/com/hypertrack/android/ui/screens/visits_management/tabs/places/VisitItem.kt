package com.hypertrack.android.ui.screens.visits_management.tabs.places

import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.utils.Meters
import com.hypertrack.android.utils.datetime.prettyFormat
import java.time.LocalDate

sealed class VisitItem
class Visit(
    val day: LocalDate,
    val geofenceId: String,
    val visitId: String,
    val title: String,
    val durationText: String?,
    val routeToText: String?,
    val integrationName: String?,
    val addressText: String
) : VisitItem() {
    override fun toString(): String {
        return "visit ${day.prettyFormat()}"
    }
}

class Day(val date: LocalDate, val totalDriveDistance: Meters) : VisitItem() {
    override fun toString(): String {
        return date.prettyFormat()
    }
}
