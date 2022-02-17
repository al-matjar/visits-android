package com.hypertrack.android.ui.screens.sign_in.use_case

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.screens.sign_in.SignInFragmentDirections
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.login.CognitoException
import com.hypertrack.android.use_case.login.CognitoLoginError
import com.hypertrack.android.use_case.login.EmailConfirmationRequired
import com.hypertrack.android.use_case.login.InvalidLoginOrPassword
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.use_case.login.NoSuchUser
import com.hypertrack.android.use_case.login.SignInUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class SignInWithCognitoUseCase(
    private val signInUseCase: SignInUseCase,
    private val resourceProvider: ResourceProvider
) {

    fun execute(
        login: String, password: String
    ): Flow<Result<SignInResult>> {
        return signInUseCase.execute(email = login, password = password)
            .map { result: AbstractResult<LoggedIn, CognitoLoginError> ->
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
                                Exception(
                                    resourceProvider.stringFromResource(
                                        R.string.user_does_not_exist
                                    )
                                ).asFailure()
                            }
                            is InvalidLoginOrPassword -> {
                                Exception(
                                    resourceProvider.stringFromResource(
                                        R.string.incorrect_username_or_pass
                                    )
                                ).asFailure()
                            }
                            is CognitoException -> {
                                Exception(
                                    resourceProvider.stringFromResource(
                                        R.string.unknown_error
                                    )
                                ).asFailure()
                            }
                        }
                    }
                }
            }
    }

}

// todo separate class
sealed class SignInResult
data class SignInSuccess(val loggedIn: LoggedIn) : SignInResult()
object ConfirmationRequired : SignInResult()

