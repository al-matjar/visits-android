package com.hypertrack.android.deeplink

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import io.branch.referral.Branch
import io.branch.referral.BranchError
import org.json.JSONObject

class BranchWrapper(
    private val crashReportsProvider: CrashReportsProvider
) {

    // this method should be executed right after onStart, without any delays
    // e.g. should not be ran in the coroutine
    fun activityOnStart(
        activity: Activity,
        uri: Uri?,
        callback: (DeeplinkResult) -> Unit
    ) {
        handleDeeplink(
            activity, uri, reInitBranchSession = false, callback
        )
    }

    fun activityOnNewIntent(
        activity: Activity,
        intent: Intent?,
        callback: (DeeplinkResult) -> Unit
    ) {
        // if activity is in foreground (or in backstack but partially visible) launching the same
        // activity will skip onStart, handle this case with reInitSession
        if (intent != null &&
            intent.hasExtra(KEY_BRANCH_FORCE_NEW_SESSION) &&
            intent.getBooleanExtra(KEY_BRANCH_FORCE_NEW_SESSION, false)
        ) {
            handleDeeplink(
                activity, intent.data, reInitBranchSession = true, callback
            )
        }
    }

    fun handleGenericDeeplink(
        activity: Activity,
        intent: Intent,
        uri: Uri,
        callback: (DeeplinkResult) -> Unit
    ) {
        // branch requires this extra to reInit session
        intent.putExtra(KEY_BRANCH_FORCE_NEW_SESSION, true)
        handleDeeplink(
            activity,
            uri,
            reInitBranchSession = true,
            callback
        )
    }

    private fun handleDeeplink(
        activity: Activity,
        uri: Uri?,
        reInitBranchSession: Boolean,
        callback: (DeeplinkResult) -> Unit
    ) {
        try {
            crashReportsProvider.log("Checking for deeplink")
            uri?.let {
                crashReportsProvider.log("got deeplink $uri")
            }
            Branch.sessionBuilder(activity)
                .withCallback { branchObject, error ->
                    handleBranchCallback(branchObject, error, callback)
                }
                .withData(uri)
                .apply {
                    if (reInitBranchSession) {
                        reInit()
                    } else {
                        init()
                    }
                }
        } catch (e: Exception) {
            callback.invoke(DeeplinkError(e))
        }
    }

    private fun handleBranchCallback(
        branchObject: JSONObject?,
        error: BranchError?,
        callback: (DeeplinkResult) -> Unit
    ) {
        try {
            crashReportsProvider.log(
                mapOf(
                    "branchObject" to branchObject,
                    "error" to error
                ).toString()
            )
            callback.invoke(
                if (error != null) {
                    when (error.errorCode) {
                        -118 -> {
                            // ignoring
                            // "-118: Warning. Session initialization already happened. To force a new session, set intent extra, "branch_force_new_session", to true."
                            // https://github.com/BranchMetrics/android-branch-deep-linking-attribution/issues/839
                            NoDeeplink
                        }
                        else -> {
                            DeeplinkError(
                                BranchErrorException(
                                    error.errorCode,
                                    error.message
                                )
                            )
                        }
                    }
                } else {
                    if (branchObject == null || !branchObject.getBoolean(
                            KEY_CLICKED_BRANCH_LINK
                        )
                    ) {
                        NoDeeplink
                    } else {
                        DeeplinkParams(branchObject.toStringMap())
                    }
                }
            )
        } catch (e: Exception) {
            callback.invoke(DeeplinkError(e))
        }
    }

    companion object {
        const val KEY_BRANCH_FORCE_NEW_SESSION = "branch_force_new_session"
        private const val KEY_CLICKED_BRANCH_LINK = "+clicked_branch_link"
        const val BRANCH_TIMEOUT = 60000
        private const val BRANCH_CONNECTION_TIMEOUT = 30000
        private const val BRANCH_INTERVAL_BETWEEN_RETRIES = 1000
        private const val BRANCH_RETRY_COUNT = 3

        fun init(application: Application) {
//            if(MyApplication.DEBUG_MODE) {
//                 Branch.enableLogging();
//            }
            Branch.getAutoInstance(application).apply {
                setNetworkTimeout(BRANCH_TIMEOUT)
                setNetworkConnectTimeout(BRANCH_CONNECTION_TIMEOUT)
                setRetryCount(BRANCH_RETRY_COUNT)
                setRetryInterval(BRANCH_INTERVAL_BETWEEN_RETRIES)
            }
        }
    }
}

private fun JSONObject.toStringMap(): Map<String, String> {
    return mutableMapOf<String, String>().also { map ->
        keys().forEach { key ->
            get(key).let { value ->
                if (value is String) {
                    map[key] = value
                }
            }
        }
    }
}

