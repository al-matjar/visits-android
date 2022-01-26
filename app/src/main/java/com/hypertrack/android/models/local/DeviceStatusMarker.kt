package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarker
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerActiveData
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerInactiveData
import com.hypertrack.android.ui.common.util.nullIfZero
import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.Meters
import com.hypertrack.android.utils.Steps
import com.hypertrack.android.utils.datetime.ClosedDateTimeRange
import com.hypertrack.android.utils.datetime.DateTimeRange
import com.hypertrack.android.utils.datetime.EndDateTime
import com.hypertrack.android.utils.datetime.OpenDateTimeRange
import com.hypertrack.android.utils.datetime.StartDateTime
import com.hypertrack.android.utils.toSteps

sealed class DeviceStatusMarker(
    val ongoingStatus: OngoingStatus
) {
    companion object {
        fun fromGraphQl(gqlMarker: GraphQlDeviceStatusMarker): DeviceStatusMarker {
            return when (gqlMarker.data) {
                is GraphQlDeviceStatusMarkerActiveData -> {
                    DeviceStatusMarkerActive(
                        ongoingStatus = getOngoingStatus(gqlMarker),
                        locationEdges = (gqlMarker.data.startLocation?.toLatLng() to
                                gqlMarker.data.endLocation?.toLatLng()),
                        address = gqlMarker.data.endAddress?.address
                            ?: gqlMarker.data.startAddress?.address,
                        distance = gqlMarker.data.distance?.nullIfZero()?.let { Meters(it) },
                        activity = gqlMarker.data.activity?.let {
                            UserActivity.fromString(it)
                        } ?: UnknownActivity,
                        steps = gqlMarker.data.steps?.nullIfZero()?.toSteps()
                    )
                }
                is GraphQlDeviceStatusMarkerInactiveData -> {
                    DeviceStatusMarkerInactive(
                        getOngoingStatus(gqlMarker),
                        OutageReason.fromString(gqlMarker.data.reason)
                    )
                }
            }
        }

        private fun getOngoingStatus(gqlMarker: GraphQlDeviceStatusMarker): OngoingStatus {
            return if (gqlMarker.endDatetime != null) {
                MarkerEnded(
                    ClosedDateTimeRange(
                        StartDateTime(gqlMarker.startDatetime),
                        EndDateTime(gqlMarker.endDatetime)
                    )
                )
            } else {
                MarkerOngoing(OpenDateTimeRange(StartDateTime(gqlMarker.startDatetime)))
            }
        }
    }
}

class DeviceStatusMarkerActive(
    ongoingStatus: OngoingStatus,
    val locationEdges: Pair<StartLocation?, EndLocation?>,
    val address: String?,
    val distance: DistanceValue?,
    val activity: UserActivity,
    val steps: Steps?
) : DeviceStatusMarker(
    ongoingStatus
)

class DeviceStatusMarkerInactive(
    ongoingStatus: OngoingStatus,
    val reason: OutageReason
) : DeviceStatusMarker(
    ongoingStatus
)

sealed class OngoingStatus(private val _dateTimeRange: DateTimeRange) {
    fun getDateTimeRange(): DateTimeRange = _dateTimeRange
}

class MarkerEnded(val datetimeRange: ClosedDateTimeRange) : OngoingStatus(datetimeRange)
class MarkerOngoing(val datetimeRange: OpenDateTimeRange) : OngoingStatus(datetimeRange)

typealias StartLocation = LatLng
typealias EndLocation = LatLng
