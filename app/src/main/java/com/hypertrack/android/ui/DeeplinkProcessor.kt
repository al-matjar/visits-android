package com.hypertrack.android.utils

import android.app.Activity
import android.app.Application
import android.net.Uri
import com.hypertrack.android.utils.DeeplinkProcessor.Companion.DEEPLINK_REGEX
import io.branch.referral.Branch
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface DeeplinkProcessor {
    fun appOnCreate(application: Application)
    suspend fun activityOnStart(activity: Activity): DeeplinkResult
    suspend fun activityOnNewIntent(activity: Activity): DeeplinkResult
    suspend fun onLinkRetrieved(activity: Activity, link: String): DeeplinkResult

    companion object {
        val DEEPLINK_REGEX = Pattern
            .compile("https:\\/\\/hypertrack-logistics\\.app\\.link\\/(.+)(\\?.*)?")
    }
}

sealed class DeeplinkResult
object NoDeeplink : DeeplinkResult() {
    override fun toString(): String = javaClass.simpleName
}

data class DeeplinkParams(val parameters: Map<String, Any>) : DeeplinkResult()
data class DeeplinkError(val exception: Exception) : DeeplinkResult() {}


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
                action.activity.intent?.let {
                    it.data?.let { uri ->
                        handleLink(action.activity, uri, action.reInit)
                    } ?: NoDeeplink
                } ?: NoDeeplink
            }
            is DeeplinkPasted -> {
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

        return initBranchSession(activity, uri, reInit).let { branchResult ->
            when (branchResult) {
                is BranchSuccess -> {
                    DeeplinkParams(branchResult.metadata)
                }
                is com.hypertrack.android.utils.BranchError -> {
                    DeeplinkError(branchResult.exception)
                }
            }
        }
    }

    private suspend fun initBranchSession(
        activity: Activity,
        link: Uri,
        reInit: Boolean
    ): BranchResult = suspendCoroutine { continuation ->
        branch.initSession(activity, link, reInit) { branchResult ->
            continuation.resume(branchResult)
        }
    }

}

sealed class BranchResult
class BranchSuccess(val metadata: Map<String, String>) : BranchResult()
class BranchError(val exception: Exception) : BranchResult()

class BranchWrapper {
    fun getAutoInstance(application: Application) {
        Branch.getAutoInstance(application)
            .setNetworkTimeout(BRANCH_CONNECTION_TIMEOUT)
    }

    fun initSession(
        activity: Activity,
        link: Uri,
        reInit: Boolean,
        callback: (BranchResult) -> Unit
    ) {
        try {
            Branch.sessionBuilder(activity)
                .withCallback { branchUniversalObject, _, error ->
                    callback.invoke(
                        if (error != null) {
                            BranchError(BranchErrorException(error.errorCode, error.message))
                        } else {
                            branchUniversalObject?.contentMetadata?.customMetadata?.let {
                                BranchSuccess(it)
                            } ?: BranchError(Exception("branch metadata == null"))
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
            callback.invoke(BranchError(e))
        }
    }

    companion object {
        private const val BRANCH_CONNECTION_TIMEOUT = 30000
    }

}

class InvalidDeeplinkFormat(link: String) : Exception("Invalid url format: $link")
class BranchErrorException(
    val code: Int,
    val branchMessage: String
) : Exception("$code: $branchMessage")

sealed class Action
data class DeeplinkPasted(val activity: Activity, val link: String) : Action()
data class IntentReceived(
    val activity: Activity,
    val reInit: Boolean
) : Action()


