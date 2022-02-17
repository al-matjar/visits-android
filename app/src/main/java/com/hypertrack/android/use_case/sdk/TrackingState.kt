package com.hypertrack.android.use_case.sdk

// todo rename
sealed class NewTrackingState {
    override fun toString(): String = javaClass.simpleName
}

object TrackingStateUnknown : NewTrackingState()
object TrackingStarted : NewTrackingState()
object TrackingStopped : NewTrackingState()
object DeviceDeleted : NewTrackingState()
object PermissionsDenied : NewTrackingState()
object LocationServicesDisabled : NewTrackingState()
data class TrackingFailure(
    val code: Int,
    val message: String,
    val codeName: String
) : NewTrackingState()
