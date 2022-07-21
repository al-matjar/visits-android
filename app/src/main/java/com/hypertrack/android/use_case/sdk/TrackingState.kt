package com.hypertrack.android.use_case.sdk

sealed class TrackingState {
    fun isTracking(): Boolean {
        return this is TrackingStarted
    }

    override fun toString(): String = javaClass.simpleName
}

object TrackingStateUnknown : TrackingState()
object TrackingStarted : TrackingState()
object TrackingStopped : TrackingState()
object DeviceDeleted : TrackingState()
object PermissionsDenied : TrackingState()
object LocationServicesDisabled : TrackingState()
data class TrackingFailure(
    val code: Int,
    val message: String,
    val codeName: String
) : TrackingState()
