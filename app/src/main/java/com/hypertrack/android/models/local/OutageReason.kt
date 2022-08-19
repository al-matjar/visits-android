package com.hypertrack.android.models.local

import androidx.annotation.StringRes
import com.hypertrack.logistics.android.github.R

sealed class OutageReason(@StringRes val stringRes: Int) {
    companion object {
        fun fromString(reason: String): OutageReason {
            return when (reason) {
                OUTAGE_MARKER_LOCATION_PERMISSIONS_DENIED -> {
                    LocationPermissionsDenied
                }
                OUTAGE_MARKER_LOCATION_SERVICES_DISABLED -> {
                    LocationServicesDisabled
                }
                OUTAGE_MARKER_MOTION_ACTIVITY_PERMISSIONS_DENIED -> {
                    ActivityPermissionsDenied
                }
                OUTAGE_MARKER_MOTION_ACTIVITY_SERVICES_DISABLED -> {
                    ActivityServicesDisabled
                }
                OUTAGE_MARKER_MOTION_ACTIVITY_SERVICES_UNAVAILABLE -> {
                    ActivityServicesUnavailable
                }
                OUTAGE_MARKER_TRACKING_STOPPED,
                OUTAGE_MARKER_STOPPED_PROGRAMMATICALLY -> {
                    TrackingStopped
                }
                OUTAGE_MARKER_TRACKING_SERVICE_TERMINATED -> {
                    SdkKilled
                }
                OUTAGE_MARKER_SDK_KILLED_BY_OS_REBOOT -> {
                    SdkKilledByReboot
                }
                OUTAGE_MARKER_LOCATION_UNAVAILABLE -> {
                    LocationUnavailable
                }
                OUTAGE_MARKER_LOCATION_PERMISSION_WHEN_IN_USE_BACKGROUND -> {
                    BackgroundLocationPermissionsDenied
                }
                OUTAGE_MARKER_SDK_KILLED_CRASHED -> {
                    SdkKilledCrashed
                }
                OUTAGE_MARKER_SDK_KILLED_EXCESSIVE_RESOURCE_USAGE -> {
                    SdkKilledExcessiveResourcesUsage
                }
                OUTAGE_MARKER_SDK_KILLED_BY_USER -> {
                    SdkKilledByUser
                }
                OUTAGE_MARKER_DISCONNECTED -> {
                    Disconnected
                }
                else -> {
                    Unknown(reason)
                }
            }
        }

        private const val OUTAGE_MARKER_LOCATION_PERMISSIONS_DENIED = "location_permissions_denied"
        private const val OUTAGE_MARKER_LOCATION_SERVICES_DISABLED = "location_services_disabled"
        private const val OUTAGE_MARKER_MOTION_ACTIVITY_PERMISSIONS_DENIED =
            "motion_activity_permissions_denied"
        private const val OUTAGE_MARKER_MOTION_ACTIVITY_SERVICES_DISABLED =
            "motion_activity_services_disabled"
        private const val OUTAGE_MARKER_MOTION_ACTIVITY_SERVICES_UNAVAILABLE =
            "motion_activity_services_unavailable"
        private const val OUTAGE_MARKER_TRACKING_STOPPED = "tracking_stopped"
        private const val OUTAGE_MARKER_STOPPED_PROGRAMMATICALLY = "stopped_programmatically"
        private const val OUTAGE_MARKER_TRACKING_SERVICE_TERMINATED = "tracking_service_terminated"
        private const val OUTAGE_MARKER_SDK_KILLED_BY_OS_REBOOT = "sdk_killed_by_os_reboot"
        private const val OUTAGE_MARKER_LOCATION_UNAVAILABLE = "location_unavailable"
        private const val OUTAGE_MARKER_LOCATION_PERMISSION_WHEN_IN_USE_BACKGROUND =
            "location_permission_when_in_use_background"
        private const val OUTAGE_MARKER_SDK_KILLED_CRASHED = "sdk_killed_crashed"
        private const val OUTAGE_MARKER_SDK_KILLED_EXCESSIVE_RESOURCE_USAGE =
            "sdk_killed_excessive_resource_usage"
        private const val OUTAGE_MARKER_SDK_KILLED_BY_USER = "sdk_killed_by_user"
        private const val OUTAGE_MARKER_DISCONNECTED = "disconnected"
    }
}

object LocationServicesDisabled :
    OutageReason(R.string.timeline_inactive_reason_location_services_disabled)

object LocationPermissionsDenied :
    OutageReason(R.string.timeline_inactive_reason_location_permissions_denied)

object BackgroundLocationPermissionsDenied :
    OutageReason(R.string.timeline_inactive_reason_location_permission_when_in_use_background)

object ActivityPermissionsDenied :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_permissions_denied)

object ActivityServicesDisabled :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_services_disabled)

// iOS only?
object ActivityServicesUnavailable :
    OutageReason(R.string.timeline_inactive_reason_motion_activity_services_unavailable)

object LocationUnavailable :
    OutageReason(R.string.timeline_inactive_reason_location_unavailable)

object Disconnected :
    OutageReason(R.string.timeline_inactive_reason_disconnected)

object TrackingStopped :
    OutageReason(R.string.timeline_inactive_reason_tracking_stopped)

object SdkKilled :
    OutageReason(R.string.timeline_inactive_reason_tracking_service_terminated)

object SdkKilledByReboot :
    OutageReason(R.string.timeline_inactive_reason_tracking_service_terminated_by_reboot)

object SdkKilledExcessiveResourcesUsage :
    OutageReason(R.string.timeline_inactive_reason_tracking_service_terminated)

object SdkKilledByUser :
    OutageReason(R.string.timeline_inactive_reason_sdk_killed_by_user)

object SdkKilledCrashed :
    OutageReason(R.string.timeline_inactive_reason_sdk_killed_crashed)

class Unknown(val reason: String) : OutageReason(R.string.timeline_inactive_reason_unexpected)
