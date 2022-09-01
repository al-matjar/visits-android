package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.repository.user.UserData
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
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class SignInUseCase(
    private val getPublishableKeyWithCognitoUseCase: GetPublishableKeyWithCognitoUseCase,
    private val loginWithPublishableKeyUseCase: LoginWithPublishableKeyUseCase,
    private val getConfiguredHypertrackSdkInstanceUseCase: GetConfiguredHypertrackSdkInstanceUseCase,
) {

    fun execute(
        email: String,
        password: String,
    ): Flow<AbstractResult<LoggedIn, CognitoLoginError>> {
        return getPublishableKeyWithCognitoUseCase.execute(
            email.lowercase(Locale.getDefault()),
            password
        )
            .flatMapConcat { res ->
                when (res) {
                    is AbstractSuccess -> {
                        val publishableKey = res.success
                        getConfiguredHypertrackSdkInstanceUseCase.execute(res.success)
                            .flatMapConcat { hyperTrackSdk ->
                                loginWithPublishableKeyUseCase.execute(
                                    hyperTrackSdk,
                                    UserData.fromUserAuthData(
                                        EmailAuthData(
                                            Email(email),
                                            publishableKey,
                                            mapOf(),
                                        )
                                    ),
                                    publishableKey,
                                    deeplinkWithoutGetParams = null,
                                )
                            }
                            .map {
                                when (it) {
                                    is Success -> AbstractSuccess(it.data)
                                    is Failure -> AbstractFailure(CognitoException(it.exception))
                                }
                            }
                    }
                    is AbstractFailure -> {
                        flowOf(AbstractFailure(res.failure))
                    }
                }
            }
    }

}
