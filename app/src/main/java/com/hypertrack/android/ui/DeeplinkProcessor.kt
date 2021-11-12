package com.hypertrack.android.utils

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import io.branch.referral.Branch
import io.branch.referral.BranchError
import java.util.regex.Matcher
import java.util.regex.Pattern

interface DeeplinkProcessor {
    fun appOnCreate(application: Application)
    fun activityOnStart(activity: Activity, resultListener: DeeplinkResultListener)
    fun activityOnNewIntent(
        activity: Activity,
        resultListener: DeeplinkResultListener
    )

    fun onLinkRetrieved(activity: Activity, link: String, resultListener: DeeplinkResultListener)
}

interface DeeplinkResultListener {
    fun onDeeplinkResult(result: DeeplinkResult)
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

    private val linkRegex = Pattern.compile("https:\\/\\/hypertrack-logistics\\.app\\.link\\/.{11}")

    override fun appOnCreate(application: Application) {
        branch.getAutoInstance(application)
    }

    override fun activityOnStart(
        activity: Activity,
        resultListener: DeeplinkResultListener
    ) {
        onIntentReceived(activity, resultListener, false)
    }

    override fun activityOnNewIntent(
        activity: Activity,
        resultListener: DeeplinkResultListener
    ) {
        onIntentReceived(activity, resultListener, true)
    }

    override fun onLinkRetrieved(
        activity: Activity,
        link: String,
        resultListener: DeeplinkResultListener
    ) {
        with(linkRegex.matcher(link)) {
            if (matches()) {
                handleLink(activity, osUtilsProvider.parseUri(link), resultListener, true)
            } else {
                resultListener.onDeeplinkResult(
                    DeeplinkError(InvalidDeeplinkFormat(link).also {
                        crashReportsProvider.logException(it)
                    })
                )
            }
        }
    }

    private fun onIntentReceived(
        activity: Activity,
        resultListener: DeeplinkResultListener,
        reInit: Boolean
    ) {
        activity.intent?.let {
            it.data?.let { uri ->
                handleLink(activity, uri, resultListener, reInit)
            } ?: resultListener.onDeeplinkResult(NoDeeplink)
        } ?: resultListener.onDeeplinkResult(NoDeeplink)
    }

    private fun handleLink(
        activity: Activity,
        link: Uri,
        resultListener: DeeplinkResultListener,
        reInit: Boolean
    ) {
        try {
            if (reInit) {
                activity.intent.putExtra("branch_force_new_session", true)
            }

            branch.initSession(activity, link, reInit) { branchResult ->
                when (branchResult) {
                    is BranchSuccess -> {
                        resultListener.onDeeplinkResult(DeeplinkParams(branchResult.metadata))
                    }
                    is BranchErrorResult -> {
                        val error = branchResult.branchError
                        resultListener.onDeeplinkResult(
                            DeeplinkError(
                                Exception("${error.errorCode} ${error.message}")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
            resultListener.onDeeplinkResult(DeeplinkError(e))
        }
    }

}

sealed class BranchResult
class BranchSuccess(val metadata: Map<String, String>) : BranchResult()
class BranchErrorResult(val branchError: BranchError) : BranchResult()

class BranchWrapper {
    fun getAutoInstance(application: Application) {
        Branch.getAutoInstance(application)
    }

    fun initSession(
        activity: Activity,
        link: Uri,
        reInit: Boolean,
        callback: (BranchResult) -> Unit
    ) {
        Branch.sessionBuilder(activity)
            .withCallback { branchUniversalObject, linkProperties, error ->
                callback.invoke(
                    if (error != null) {
                        BranchErrorResult(error)
                    } else {
                        branchUniversalObject?.contentMetadata?.customMetadata?.let {
                            BranchSuccess(it)
                        } ?: throw Exception("branch metadata = null")
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
    }

}

class InvalidDeeplinkFormat(link: String) : Exception("Invalid url format: $link")