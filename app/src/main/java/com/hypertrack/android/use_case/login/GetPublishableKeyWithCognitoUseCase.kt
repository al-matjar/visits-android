package com.hypertrack.android.use_case.login

import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.AwsError
import com.hypertrack.android.utils.AwsSignInConfirmationRequired
import com.hypertrack.android.utils.AwsSignInError
import com.hypertrack.android.utils.AwsSignInSuccess
import com.hypertrack.android.utils.CognitoAccountLoginProvider
import com.hypertrack.android.utils.CognitoToken
import com.hypertrack.android.utils.CognitoTokenError
import com.hypertrack.android.utils.TokenForPublishableKeyExchangeService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@FlowPreview
class GetPublishableKeyWithCognitoUseCase(
    private val cognito: CognitoAccountLoginProvider,
    private val tokenService: TokenForPublishableKeyExchangeService
) {

    fun execute(
        email: String,
        password: String
    ): Flow<AbstractResult<RealPublishableKey, CognitoLoginError>> {
        return (suspend { getPublishableKey(email, password) }).asFlow()
    }

    private suspend fun getPublishableKey(
        login: String,
        password: String
    ): AbstractResult<RealPublishableKey, CognitoLoginError> {
        // get Cognito token
        val res = cognito.awsInitCallWrapper()
        if (res is AwsError) {
            return AbstractFailure(CognitoException(res.exception))
        }

        // Log.v(TAG, "Initialized with user State $userStateDetails")
        when (val signInResult = cognito.awsLoginCallWrapper(login, password)) {
            is AwsSignInSuccess -> {
                return when (val tokenRes = cognito.awsTokenCallWrapper()) {
                    is CognitoTokenError -> {
                        AbstractFailure(CognitoException(Exception("Failed to retrieve Cognito token")))
                    }
                    is CognitoToken -> {
                        val pk = getPublishableKeyFromToken(tokenRes.token)
                        // Log.d(TAG, "Got pk $pk")
                        AbstractSuccess(RealPublishableKey(pk))
                    }
                }
            }
            is AwsSignInError -> {
                signInResult.exception.let {
                    return when (it) {
                        is UserNotFoundException -> {
                            NoSuchUser
                        }
                        is NotAuthorizedException -> {
                            InvalidLoginOrPassword
                        }
                        else -> {
                            CognitoException(it)
                        }
                    }.let { AbstractFailure(it) }
                }
            }
            is AwsSignInConfirmationRequired -> {
                return AbstractFailure(EmailConfirmationRequired)
            }
        }
    }

    private suspend fun getPublishableKeyFromToken(token: String): String {
        try {
            val response = tokenService.getPublishableKey(token)
            if (response.isSuccessful) return response.body()?.publishableKey ?: ""
            return ""
        } catch (e: Exception) {
            return ""
        }
    }

}

sealed class CognitoLoginError
object NoSuchUser : CognitoLoginError()
object EmailConfirmationRequired : CognitoLoginError()
object InvalidLoginOrPassword : CognitoLoginError()
class CognitoException(val exception: Exception) : CognitoLoginError()
