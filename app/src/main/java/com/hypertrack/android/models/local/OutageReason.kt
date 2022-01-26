package com.hypertrack.android.models.local

import androidx.annotation.StringRes
import com.hypertrack.logistics.android.github.R

sealed class OutageReason(@StringRes val stringRes: Int) {
    companion object {
        fun fromString(reason: String): OutageReason {
            return when (reason) {
                "location_permissions_denied" -> {
                    LocationPermissionsDenied
                }
                "location_services_disabled" -> {
                    LocationServicesDisabled
                }
                "motion_activity_permissions_denied" -> {
                    MotionActivityPermissionsDenied
                }
                "motion_activity_services_disabled" -> {
                    MotionActivityServicesDisabled
                }
                "motion_activity_services_unavailable" -> {
                    MotionActivityServicesUnavailable
                }
                "tracking_stopped" -> {
                    TrackingStopped
                }
                "tracking_service_terminated" -> {
                    TrackingServiceTerminated
                }
                "unexpected" -> {
                    Unexpected
                }
                else -> Unknown(reason)
            }
        }
    }
}

object LocationPermissionsDenied :
    OutageReason(R.string.timeline_inactive_reason_location_permissions_denied)

object LocationServicesDisabled :
    OutageReason(R.string.timeline_inactive_reason_location_services_disabled)

object MotionActivityPermissionsDenied :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_permissions_denied)

object MotionActivityServicesDisabled :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_services_disabled)

object MotionActivityServicesUnavailable :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_services_unavailable)

object TrackingStopped :
    OutageReason(R.string.timeline_inactive_reason_tracking_stopped)

object TrackingServiceTerminated :
    OutageReason(R.string.timeline_inactive_reason_tracking_service_terminated)

object Unexpected : OutageReason(R.string.timeline_inactive_reason_unexpected)

class Unknown(val reason: String) : OutageReason(R.string.timeline_inactive_reason_unexpected)