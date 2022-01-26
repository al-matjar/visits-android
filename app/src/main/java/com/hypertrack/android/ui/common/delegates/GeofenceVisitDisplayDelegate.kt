package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.common.delegates.address.GeofenceVisitAddressDelegate
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.DateTimeRange
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.hypertrack.logistics.android.github.R

class GeofenceVisitDisplayDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val dateTimeFormatter: DateTimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val timeValueFormatter: TimeValueFormatter,
    private val addressDelegate: GeofenceVisitAddressDelegate,
    private val dateTimeRangeFormatterDelegate: DateTimeRangeFormatterDelegate
) {

    fun getGeofenceName(visit: LocalGeofenceVisit): String {
        return visit.metadata?.name
            ?: visit.metadata?.integration?.name
            ?: visit.address
            ?: visit.geofenceId
    }

    fun getVisitTimeText(visit: LocalGeofenceVisit): String {
        return dateTimeRangeFormatterDelegate.formatDatetimeRange(
            DateTimeRange.create(visit.arrival, visit.exit)
        )
    }

    fun getVisitTimeTextForTimeline(visit: LocalGeofenceVisit): String {
        return dateTimeRangeFormatterDelegate.formatTimeRange(
            DateTimeRange.create(visit.arrival, visit.exit)
        )
    }

    fun getDurationText(visit: LocalGeofenceVisit): String? {
        return visit.durationSeconds?.let { timeValueFormatter.formatSeconds(it) }
    }

    fun getRouteToText(item: LocalGeofenceVisit): String? {
        return item.routeTo?.let {
            if (it.distance == null) return@let null
            if (it.duration == null) return@let null
            MyApplication.context.getString(
                R.string.place_route_ro,
                distanceFormatter.formatDistance(it.distance),
                timeValueFormatter.formatSeconds(it.duration)
            )
        }
    }

    fun getAddress(visit: LocalGeofenceVisit): String {
        return addressDelegate.shortAddress(visit)
    }

    fun getGeofenceDescription(visit: LocalGeofenceVisit): String? {
        //todo geofence description
        return null
    }

}
