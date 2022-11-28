package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.deeplink.result.DeeplinkParamsInvalid
import com.hypertrack.android.use_case.deeplink.result.DeeplinkParamsValid
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.mapSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.hypertrack.android.utils.Result

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoginWithDeeplinkParamsUseCase(
    private val validateDeeplinkParamsUseCase: ValidateDeeplinkParamsUseCase,
) {

    fun execute(
        deeplinkParams: DeeplinkParams,
    ): Flow<Result<out AbstractResult<InitiateLoginAction, DeeplinkParamsInvalid>>> {
        return validateDeeplinkParamsUseCase.execute(deeplinkParams)
            .mapSuccess { validationResult ->
                when (validationResult) {
                    is DeeplinkParamsValid -> {
                        val publishableKey = validationResult.userAuthData.publishableKey
                        val userData = UserData.fromUserAuthData(validationResult.userAuthData)
                        AbstractSuccess(
                            InitiateLoginAction(
                                publishableKey,
                                userData
                            )
                        )
                    }
                    is DeeplinkParamsInvalid -> {
                        AbstractFailure(
                            validationResult
                        )
                    }
                }
            }
    }

}


