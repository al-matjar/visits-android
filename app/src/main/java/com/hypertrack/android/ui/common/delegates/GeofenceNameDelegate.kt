package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.models.local.LocalGeofence

import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.logistics.android.github.R

class GeofenceNameDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val dateTimeFormatter: DateTimeFormatter,
) {

    fun getName(localGeofence: LocalGeofence): String {
        return localGeofence.name
            ?: localGeofence.integration?.name
            ?: localGeofence.address
            ?: osUtilsProvider.stringFromResource(
                R.string.places_created,
                dateTimeFormatter.formatDateTime(localGeofence.createdAt)
            )
    }

}
