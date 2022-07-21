package com.hypertrack.android.deeplink

class BranchErrorException(
    val code: Int,
    branchMessage: String
) : Exception("$code: $branchMessage") {

    val isBranchConnectionError = code == CODE_CONNECTION_ERROR || code == CODE_OPERATION_TIMEOUT

    companion object {
        const val CODE_CONNECTION_ERROR = -113
        const val CODE_OPERATION_TIMEOUT = -120
        const val CODE_SESSION_REINIT_WARNING = -118
    }
}
