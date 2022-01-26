package com.hypertrack.android.ui.screens.visits_management.tabs.history

import androidx.annotation.StringRes
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit

sealed class TimelineDialog
class GeotagDialog(
    val geotagId: String,
    val title: String,
    val metadataString: String,
    val routeToText: String?,
    val address: String?,
) : TimelineDialog()

class GeofenceVisitDialog(
    val visitId: String,
    val geofenceId: String,
    val geofenceName: String,
    val geofenceDescription: String?,
    val integrationName: String?,
    val address: String?,
    val durationText: String?,
    val routeToText: String?,
) : TimelineDialog()
