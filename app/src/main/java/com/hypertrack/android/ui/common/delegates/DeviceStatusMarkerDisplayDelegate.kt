package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.models.local.DeviceStatusMarker
import com.hypertrack.android.models.local.DeviceStatusMarkerActive
import com.hypertrack.android.models.local.DeviceStatusMarkerInactive
import com.hypertrack.android.models.local.Drive
import com.hypertrack.android.models.local.LocationPermissionsDenied
import com.hypertrack.android.models.local.LocationServicesDisabled
import com.hypertrack.android.models.local.MarkerEnded
import com.hypertrack.android.models.local.MarkerOngoing
import com.hypertrack.android.models.local.MotionActivityPermissionsDenied
import com.hypertrack.android.models.local.MotionActivityServicesDisabled
import com.hypertrack.android.models.local.MotionActivityServicesUnavailable
import com.hypertrack.android.models.local.Stop
import com.hypertrack.android.models.local.TrackingServiceTerminated
import com.hypertrack.android.models.local.TrackingStopped
import com.hypertrack.android.models.local.Unknown
import com.hypertrack.android.models.local.UnknownActivity
import com.hypertrack.android.models.local.Walk
import com.hypertrack.android.ui.common.adapters.formatUnderscore
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.TimeValue
import com.hypertrack.android.utils.datetime.OpenDateTimeRange
import com.hypertrack.android.utils.datetime.timeBetween
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.hypertrack.android.utils.toMeters
import java.time.ZonedDateTime

class DeviceStatusMarkerDisplayDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val distanceFormatter: DistanceFormatter,
    private val timeValueFormatter: TimeValueFormatter,
    private val dateTimeRangeFormatterDelegate: DateTimeRangeFormatterDelegate
) {

    fun getDescription(marker: DeviceStatusMarker): String {
        return when (marker) {
            is DeviceStatusMarkerActive -> {
                val duration = when (marker.ongoingStatus) {
                    is MarkerEnded -> marker.ongoingStatus.datetimeRange.duration
                    is MarkerOngoing -> getDuration(
                        marker.ongoingStatus.datetimeRange,
                        ZonedDateTime.now()
                    )
                }
                when (marker.activity) {
                    Drive -> {
                        "${timeValueFormatter.formatTimeValue(duration)}${
                            marker.distance?.let(distanceFormatter::formatDistance)
                                ?.let { "$DIVIDER$it" }
                                ?: ""
                        }"
                    }
                    Walk -> {
                        "${
                            timeValueFormatter.formatTimeValue(duration)
                        }$DIVIDER${
                            distanceFormatter.formatDistance(
                                marker.distance ?: 0.toMeters()
                            )
                        }${
                            marker.steps?.let(distanceFormatter::formatSteps)
                                ?.let { "$DIVIDER$it" }
                                ?: ""
                        }"
                    }
                    Stop, UnknownActivity -> duration.let(timeValueFormatter::formatTimeValue)
                }
            }
            is DeviceStatusMarkerInactive -> {
                when (marker.reason) {
                    is Unknown -> {
                        crashReportsProvider.logException(
                            Exception(
                                "unknown outage reason: ${marker.reason.reason}"
                            )
                        )
                        marker.reason.reason.formatUnderscore(capitalizeOnlyFirst = true)
                    }
                    else -> {
                        osUtilsProvider.stringFromResource(marker.reason.stringRes)
                    }
                }

            }
        }
    }

    fun getAddress(marker: DeviceStatusMarker): String? {
        return when (marker) {
            is DeviceStatusMarkerActive -> marker.address
            is DeviceStatusMarkerInactive -> null
        }
    }

    fun getTimeStringForTimeline(marker: DeviceStatusMarker): String {
        return dateTimeRangeFormatterDelegate.formatTimeRange(
            marker.ongoingStatus.getDateTimeRange()
        )
    }

    private fun getDuration(
        openDatetimeRange: OpenDateTimeRange,
        currentDatetime: ZonedDateTime
    ): TimeValue {
        return timeBetween(openDatetimeRange.start.value, currentDatetime)
    }

    companion object {
        const val DIVIDER = " • "
    }

}
