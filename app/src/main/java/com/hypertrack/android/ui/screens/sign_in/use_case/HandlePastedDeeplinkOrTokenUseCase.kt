package com.hypertrack.android.ui.screens.sign_in.use_case

import android.app.Activity
import android.net.Uri
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.deeplink.BranchWrapper.Companion.BRANCH_CONNECTION_TIMEOUT
import com.hypertrack.android.deeplink.BranchWrapper.Companion.KEY_BRANCH_FORCE_NEW_SESSION
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.DeeplinkException
import com.hypertrack.android.use_case.deeplink.DeeplinkValidationError
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.use_case.deeplink.LoginWithDeeplinkParamsUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleException
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.mapFailure
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import java.util.regex.Pattern
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class HandlePastedDeeplinkOrTokenUseCase(
    private val loginWithDeeplinkParamsUseCase: LoginWithDeeplinkParamsUseCase,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
    private val branchWrapper: BranchWrapper,
    private val osUtilsProvider: OsUtilsProvider,
    private val resourceProvider: ResourceProvider,
    private val moshi: Moshi,
) {

    fun execute(
        text: String,
        activity: Activity
    ): Flow<AbstractResult<LoggedIn, DeeplinkValidationError>> {
        return {
            tryAsResult {
                with(DEEPLINK_REGEX.matcher(text)) {
                    matches()
                }
            }
        }.asFlow()
            .flatMapSuccess { isLink ->
                if (isLink) {
                    logMessageToCrashlyticsUseCase.execute(
                        "Deeplink pasted: $text"
                    ).flatMapConcat {
                        getUriFromLink(text)
                    }.flatMapSuccess { uri ->
                        getDeeplinkResult(activity, uri)
                    }
                } else {
                    parseTokenFlow(text)
                        .map { result ->
                            when (result) {
                                is Success -> {
                                    (DeeplinkParams(result.data["data"] as Map<String, Any>) as DeeplinkResult)
                                        .asSuccess()
                                }
                                is Failure -> {
                                    Failure(InvalidDeeplinkFormat(text))
                                }
                            }
                        }
                }
            }.flatMapConcat {
                when (it) {
                    is Success -> {
                        deeplinkResultReceivedFlow(it.data)
                    }
                    is Failure -> {
                        AbstractFailure<LoggedIn, DeeplinkValidationError>(
                            DeeplinkValidationError(DeeplinkException(it.exception))
                        ).toFlow()
                    }
                }
            }.flatMapConcat { result ->
                logExceptionToCrashlyticsUseCase.execute(
                    SimpleException("deeplink pasted or login token used"),
                    mapOf(
                        "pasted_data" to text,
                        "result" to result.toString()
                    )
                ).map { result }
            }
    }

    private fun deeplinkResultReceivedFlow(
        deeplinkResult: DeeplinkResult,
    ): Flow<AbstractResult<LoggedIn, DeeplinkValidationError>> {
        return when (deeplinkResult) {
            is DeeplinkParams -> {
                loginWithDeeplinkParamsUseCase.execute(deeplinkResult)
                    .flatMapConcat { res ->
                        when (res) {
                            is AbstractSuccess -> {
                                AbstractSuccess<LoggedIn, DeeplinkValidationError>(res.success).toFlow()
                            }
                            is AbstractFailure -> {
                                flowOf(
                                    AbstractFailure(
                                        DeeplinkValidationError(
                                            res.failure.failure
                                        )
                                    )
                                )
                            }
                        }
                    }

            }
            NoDeeplink -> {
                flowOf(
                    AbstractFailure(
                        DeeplinkValidationError(
                            DeeplinkException(
                                IllegalStateException("NoDeeplink")
                            )
                        )
                    )
                )
            }
            is DeeplinkError -> {
                flowOf(
                    AbstractFailure(
                        DeeplinkValidationError(DeeplinkException(deeplinkResult.exception))
                    )
                )
            }
        }
    }

    private fun parseTokenFlow(loginToken: String): Flow<Result<Map<String, Any>>> {
        return {
            tryAsResult {
                osUtilsProvider.decodeBase64(
                    if (loginToken.contains("?")) {
                        loginToken.split("?")[0]
                    } else {
                        loginToken
                    }
                ).let { json ->
                    moshi.createAnyMapAdapter().fromJson(json)!!
                }
            }
        }.asFlow()
    }

    private fun getUriFromLink(link: String): Flow<Result<Uri>> {
        return {
            with(DEEPLINK_REGEX.matcher(link)) {
                if (matches()) {
                    // trim to avoid branch.io issue if the link have space in the end
                    osUtilsProvider.parseUri(link.trim()).asSuccess()
                } else {
                    Failure(InvalidDeeplinkFormat(link))
                }
            }
        }.asFlow()
    }

    private fun getDeeplinkResult(activity: Activity, uri: Uri): Flow<Result<DeeplinkResult>> {
        return suspend {
            withTimeout(BRANCH_CONNECTION_TIMEOUT.toLong()) {
                suspendCoroutine<Result<DeeplinkResult>> { continuation ->
                    branchWrapper.handleGenericDeeplink(
                        activity,
                        activity.intent,
                        uri
                    ) {
                        continuation.resume(it.asSuccess())
                    }
                }
            }
        }.asFlow()
    }


    @Suppress("RegExpRedundantEscape")
    companion object {
        val DEEPLINK_REGEX: Pattern = Pattern
            .compile("https:\\/\\/hypertrack-logistics\\.app\\.link\\/(.+)(\\?.*)?")
    }

}

class InvalidDeeplinkFormat(link: String) : Exception("Invalid url format: $link")

