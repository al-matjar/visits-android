package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ConfirmationRequired
import com.hypertrack.android.ui.screens.sign_in.use_case.result.SignInInvalidLoginOrPassword
import com.hypertrack.android.ui.screens.sign_in.use_case.result.SignInNoSuchUser
import com.hypertrack.android.ui.screens.sign_in.use_case.result.SignInResult
import com.hypertrack.android.ui.screens.sign_in.use_case.result.SignInSuccess
import com.hypertrack.android.use_case.login.CognitoException
import com.hypertrack.android.use_case.login.CognitoLoginError
import com.hypertrack.android.use_case.login.EmailConfirmationRequired
import com.hypertrack.android.use_case.login.InvalidLoginOrPassword
import com.hypertrack.android.use_case.login.NoSuchUser
import com.hypertrack.android.use_case.login.SignInUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class SignInWithCognitoUseCase(
    private val signInUseCase: SignInUseCase
) {

    fun execute(
        login: String, password: String
    ): Flow<Result<SignInResult>> {
        return signInUseCase.execute(email = login, password = password)
            .map { result: AbstractResult<InitiateLoginAction, CognitoLoginError> ->
                when (result) {
                    is AbstractSuccess -> {
                        SignInSuccess(result.success).asSuccess()
                    }
                    is AbstractFailure -> {
                        when (result.failure) {
                            is EmailConfirmationRequired -> {
                                ConfirmationRequired.asSuccess()
                            }
                            is NoSuchUser -> {
                                 SignInNoSuchUser.asSuccess()
                            }
                            is InvalidLoginOrPassword -> {
                                SignInInvalidLoginOrPassword.asSuccess()
                            }
                            is CognitoException -> {
                                result.failure.exception.asFailure()
                            }
                        }
                    }
                }
            }
    }

}

