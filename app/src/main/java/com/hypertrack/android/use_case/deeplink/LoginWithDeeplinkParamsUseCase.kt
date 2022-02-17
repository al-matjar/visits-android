package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.use_case.login.LoginWithPublishableKeyUseCase
import com.hypertrack.android.use_case.sdk.GetConfiguredHypertrackSdkInstanceUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoginWithDeeplinkParamsUseCase(
    private val validateDeeplinkUseCase: ValidateDeeplinkUseCase,
    private val getConfiguredHypertrackSdkInstanceUseCase: GetConfiguredHypertrackSdkInstanceUseCase,
    private val loginWithPublishableKeyUseCase: LoginWithPublishableKeyUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
) {

    fun execute(
        deeplinkParams: DeeplinkParams,
    ): Flow<AbstractResult<LoggedIn, DeeplinkValidationError>> {
        return validateDeeplinkUseCase.execute(deeplinkParams)
            .flatMapConcat { validationResult ->
                when (validationResult) {
                    is DeeplinkValid -> {
                        val publishableKey = validationResult.userAuthData.publishableKey
                        getConfiguredHypertrackSdkInstanceUseCase.execute(publishableKey)
                            .flatMapConcat { hypertrackSdk ->
                                loginWithPublishableKeyUseCase.execute(
                                    hypertrackSdk,
                                    UserData.fromUserAuthData(validationResult.userAuthData),
                                    publishableKey,
                                    validationResult.deeplinkWithoutGetParams
                                ).map {
                                    when (it) {
                                        is Success -> {
                                            AbstractSuccess(it.data)
                                        }
                                        is Failure -> {
                                            AbstractFailure(
                                                DeeplinkValidationError(
                                                    DeeplinkException(it.exception)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                    }
                    is DeeplinkValidationError -> {
                        flowOf<AbstractFailure<LoggedIn, DeeplinkValidationError>>(
                            AbstractFailure(
                                validationResult
                            )
                        )
                            .onEach {
                                val error = it.failure.failure
                                val exception = error.toException()
                                logExceptionToCrashlyticsUseCase.execute(exception)
                            }
                    }
                }
            }
    }

}


