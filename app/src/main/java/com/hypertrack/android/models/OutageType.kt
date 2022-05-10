package com.hypertrack.android.models

sealed class OutageType(val name: String) {
    companion object {
        val SERVICE_TERMINATED_GROUP = listOf(
            ServiceTerminated,
            ServiceTerminatedByUser,
            ServiceTerminatedByOs,
        )
    }
}

object ServiceTerminated : OutageType(
    OUTAGE_GROUP_SERVICE_TERMINATED
)

object ServiceTerminatedByUser : OutageType(
    OUTAGE_GROUP_SERVICE_TERMINATED_BY_USER
)

object ServiceTerminatedByOs : OutageType(
    OUTAGE_GROUP_SERVICE_TERMINATED_BY_OS
)

private const val OUTAGE_GROUP_SERVICE_TERMINATED = "service_terminated"
private const val OUTAGE_GROUP_SERVICE_TERMINATED_BY_USER = "service_terminated_by_user"
private const val OUTAGE_GROUP_SERVICE_TERMINATED_BY_OS = "service_terminated_by_os"
