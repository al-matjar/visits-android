package com.hypertrack.android.deeplink

import com.hypertrack.android.utils.exception.BaseException

class BranchErrorException(
    val code: Int,
    branchMessage: String
) : BaseException("$code: $branchMessage") {

    val isBranchConnectionError = code == CODE_CONNECTION_ERROR || code == CODE_OPERATION_TIMEOUT

    companion object {
        const val CODE_CONNECTION_ERROR = -113
        const val CODE_OPERATION_TIMEOUT = -120
        const val CODE_SESSION_REINIT_WARNING = -118
    }
}
