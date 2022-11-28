package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.AbstractResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class SignInUseCase(
    private val getPublishableKeyWithCognitoUseCase: GetPublishableKeyWithCognitoUseCase
) {

    fun execute(
        email: String,
        password: String,
    ): Flow<AbstractResult<InitiateLoginAction, CognitoLoginError>> {
        return getPublishableKeyWithCognitoUseCase.execute(
            email.lowercase(Locale.getDefault()),
            password
        ).map { result: AbstractResult<RealPublishableKey, CognitoLoginError> ->
            result.mapSuccess { publishableKey ->
                val userData = UserData.fromUserAuthData(
                    EmailAuthData(
                        Email(email),
                        publishableKey,
                        mapOf(),
                    )
                )
                InitiateLoginAction(publishableKey, userData)
            }
        }
    }

}
