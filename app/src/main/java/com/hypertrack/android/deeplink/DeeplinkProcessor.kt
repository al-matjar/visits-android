package com.hypertrack.android.deeplink

import android.app.Activity
import android.app.Application
import android.net.Uri
import com.hypertrack.android.deeplink.DeeplinkProcessor.Companion.DEEPLINK_REGEX
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import io.branch.referral.BranchError
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface DeeplinkProcessor {
    fun appOnCreate(application: Application)
    suspend fun activityOnStart(activity: Activity): DeeplinkResult
    suspend fun activityOnNewIntent(activity: Activity): DeeplinkResult
    suspend fun onLinkRetrieved(activity: Activity, link: String): DeeplinkResult

    @Suppress("RegExpRedundantEscape")
    companion object {
        val DEEPLINK_REGEX: Pattern = Pattern
            .compile("https:\\/\\/hypertrack-logistics\\.app\\.link\\/(.+)(\\?.*)?")
    }
}


class BranchIoDeepLinkProcessor(
    private val crashReportsProvider: CrashReportsProvider,
    private val osUtilsProvider: OsUtilsProvider,
    private val branch: BranchWrapper,
) : DeeplinkProcessor {

    override fun appOnCreate(application: Application) {
        branch.getAutoInstance(application)
    }

    override suspend fun activityOnStart(activity: Activity): DeeplinkResult {
        return handleAction(IntentReceived(activity, false))
    }

    override suspend fun activityOnNewIntent(activity: Activity): DeeplinkResult {
        return handleAction(IntentReceived(activity, true))
    }

    override suspend fun onLinkRetrieved(
        activity: Activity,
        link: String
    ): DeeplinkResult {
        return handleAction(DeeplinkPasted(activity, link))
    }

    private suspend fun handleAction(action: Action): DeeplinkResult {
        return when (action) {
            is IntentReceived -> {
                action.activity.intent?.let { intent ->
                    intent.data?.let { uri ->
                        crashReportsProvider.log("Deeplink intent received: ${intent.data}")
                        intent.extras?.get(BRANCH_DATA_KEY)?.let {
                            crashReportsProvider.log("Branch data: $it")
                        }
                        handleLink(action.activity, uri, action.reInit)
                    } ?: NoDeeplink
                } ?: NoDeeplink
            }
            is DeeplinkPasted -> {
                crashReportsProvider.log("Deeplink pasted: ${action.link}")
                with(DEEPLINK_REGEX.matcher(action.link)) {
                    if (matches()) {
                        handleLink(action.activity, osUtilsProvider.parseUri(action.link), true)
                    } else {
                        DeeplinkError(InvalidDeeplinkFormat(action.link))
                    }
                }
            }
        }.also {
            when (it) {
                is DeeplinkError -> {
                    crashReportsProvider.logException(it.exception)
                }
                is DeeplinkParams, NoDeeplink -> {
                }
            }
        }
    }

    private suspend fun handleLink(activity: Activity, uri: Uri, reInit: Boolean): DeeplinkResult {
        if (reInit) {
            activity.intent.putExtra("branch_force_new_session", true)
        }

        return initBranchSession(
            activity,
            uri,
            reInit
        ).let { branchResult: Result<BranchMetadata> ->
            when (branchResult) {
                is Success -> {
                    DeeplinkParams(branchResult.result)
                }
                is Failure -> {
                    DeeplinkError(branchResult.exception)
                }
            }
        }
    }

    private suspend fun initBranchSession(
        activity: Activity,
        link: Uri,
        reInit: Boolean
    ): Result<BranchMetadata> = suspendCoroutine { continuation ->
        branch.initSession(activity, link, reInit) { branchResult ->
            continuation.resume(branchResult)
        }
    }

    companion object {
        private const val BRANCH_DATA_KEY = "branch_data"
    }

}

class InvalidDeeplinkFormat(link: String) : Exception("Invalid url format: $link")
