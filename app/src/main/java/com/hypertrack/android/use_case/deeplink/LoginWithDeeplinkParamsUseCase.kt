package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoginWithDeeplinkParamsUseCase(
    private val validateDeeplinkUseCase: ValidateDeeplinkUseCase,
) {

    fun execute(
        deeplinkParams: DeeplinkParams,
    ): Flow<AbstractResult<InitiateLoginAction, DeeplinkValidationError>> {
        return validateDeeplinkUseCase.execute(deeplinkParams)
            .map { validationResult ->
                when (validationResult) {
                    is DeeplinkValid -> {
                        val publishableKey = validationResult.userAuthData.publishableKey
                        val userData = UserData.fromUserAuthData(validationResult.userAuthData)
                        AbstractSuccess(
                            InitiateLoginAction(
                                publishableKey,
                                userData
                            )
                        )
                    }
                    is DeeplinkValidationError -> {
                        AbstractFailure(
                            validationResult
                        )
                    }
                }
            }
    }

}


