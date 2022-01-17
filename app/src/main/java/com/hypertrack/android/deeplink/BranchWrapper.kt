package com.hypertrack.android.deeplink

import android.app.Activity
import android.app.Application
import android.net.Uri
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import io.branch.referral.Branch
import io.branch.referral.BranchError

class BranchWrapper {
    fun getAutoInstance(application: Application) {
        Branch.getAutoInstance(application)
            .setNetworkTimeout(BRANCH_CONNECTION_TIMEOUT)
    }

    fun initSession(
        activity: Activity,
        link: Uri,
        reInit: Boolean,
        callback: (Result<BranchMetadata>) -> Unit
    ) {
        try {
            Branch.sessionBuilder(activity)
                .withCallback { branchUniversalObject, linkProperties, error ->
                    callback.invoke(
                        if (error != null) {
                            Failure(BranchErrorException(error.errorCode, error.message))
                        } else {
                            branchUniversalObject?.contentMetadata?.customMetadata?.let {
                                Success(it)
                            } ?: Failure(Exception("branch metadata == null"))
                        }
                    )
                }
                .withData(link).apply {
                    if (reInit) {
                        reInit()
                    } else {
                        init()
                    }
                }
        } catch (e: Exception) {
            callback.invoke(Failure(e))
        }
    }

    companion object {
        private const val BRANCH_CONNECTION_TIMEOUT = 30000
    }

}

typealias BranchMetadata = Map<String, String>

class BranchErrorException(
    val code: Int,
    val branchMessage: String
) : Exception("$code: $branchMessage")
