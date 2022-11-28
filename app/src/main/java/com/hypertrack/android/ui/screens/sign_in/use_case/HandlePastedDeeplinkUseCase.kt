package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.ui.screens.sign_in.use_case.exception.InvalidDeeplinkFormatException
import com.hypertrack.android.ui.screens.sign_in.use_case.result.BranchReferrer
import com.hypertrack.android.ui.screens.sign_in.use_case.result.InvalidDeeplink
import com.hypertrack.android.ui.screens.sign_in.use_case.result.InvalidParams
import com.hypertrack.android.ui.screens.sign_in.use_case.result.InvalidUrl
import com.hypertrack.android.ui.screens.sign_in.use_case.result.PasteSuccess
import com.hypertrack.android.ui.screens.sign_in.use_case.result.PastedDeeplinkResult
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ValidDeeplink
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.result.WrongDeeplinkParams
import com.hypertrack.android.use_case.deeplink.result.DeeplinkParamsInvalid
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.use_case.deeplink.LoginWithDeeplinkParamsUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.exception.SimpleException
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class HandlePastedDeeplinkUseCase(
    private val validateDeeplinkUrlUseCase: ValidateDeeplinkUrlUseCase,
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase,
    private val loginWithDeeplinkParamsUseCase: LoginWithDeeplinkParamsUseCase,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
) {

    fun execute(
        text: String,
    ): Flow<Result<out PastedDeeplinkResult>> {
        return validateDeeplinkUrlUseCase.execute(text)
            .flatMapSuccess { deeplinkValidationResult ->
                when (deeplinkValidationResult) {
                    is ValidDeeplink -> {
                        logMessageToCrashlyticsUseCase.execute(
                            "Deeplink pasted: $text"
                        ).flatMapConcat {
                            getBranchDataFromAppBackendUseCase.execute(deeplinkValidationResult)
                        }.flatMapSuccess { deeplinkParams ->
                            loginWithDeeplinkParamsUseCase.execute(deeplinkParams)
                                .mapSuccess { result: AbstractResult<InitiateLoginAction, DeeplinkParamsInvalid> ->
                                    when (result) {
                                        is AbstractSuccess -> PasteSuccess(result.success)
                                        is AbstractFailure -> InvalidParams(result.failure.failure)
                                    }
                                }
                        }
                    }
                    is InvalidDeeplink -> {
                        InvalidUrl.asSuccess().toFlow()
                    }
                    is BranchReferrer -> {
                        // no need for special treatment for branch pasted referrer link (impossible value?)
                        InvalidUrl.asSuccess().toFlow()
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
}

