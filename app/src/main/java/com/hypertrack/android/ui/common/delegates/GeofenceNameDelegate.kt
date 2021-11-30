package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalGeofenceVisit

import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.logistics.android.github.R

class GeofenceNameDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val datetimeFormatter: DatetimeFormatter,
) {

    fun getName(localGeofence: LocalGeofence): String {
        return localGeofence.name
            ?: localGeofence.integration?.name
            ?: localGeofence.address
            ?: osUtilsProvider.stringFromResource(
                R.string.places_created,
                datetimeFormatter.formatDatetime(localGeofence.createdAt)
            )
    }

    fun getName(visit: LocalGeofenceVisit): String {
        return visit.metadata?.name
            ?: visit.metadata?.integration?.name
            ?: visit.address
            ?: visit.geofenceId
    }
}