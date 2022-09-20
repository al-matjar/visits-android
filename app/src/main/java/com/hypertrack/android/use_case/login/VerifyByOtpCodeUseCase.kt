package com.hypertrack.android.use_case.login

import com.hypertrack.android.api.BackendException
import com.hypertrack.android.api.LiveAccountApi
import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.sdk.GetConfiguredHypertrackSdkInstanceUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toBase64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class VerifyByOtpCodeUseCase(
    private val liveAccountUrlService: LiveAccountApi,
    private val servicesApiKey: String,
) {

    fun execute(
        email: String,
        code: String
    ): Flow<AbstractResult<InitiateLoginAction, OtpFailure>> {
        return suspend {
            verifyByOtpCode(email = email, code = code)
        }.asFlow()
            .map { verificationResult ->
                when (verificationResult) {
                    is AbstractSuccess -> {
                        val publishableKey = RealPublishableKey(verificationResult.success)
                        val userData = UserData.fromUserAuthData(
                            EmailAuthData(
                                email = Email(email),
                                publishableKey = publishableKey,
                                mapOf()
                            )
                        )
                        AbstractSuccess(InitiateLoginAction(publishableKey, userData))
                    }
                    is AbstractFailure -> {
                        AbstractFailure(verificationResult.failure)
                    }
                }
            }
    }

    private suspend fun verifyByOtpCode(
        email: String,
        code: String
    ): AbstractResult<String, OtpFailure> {
        return try {
            val res = liveAccountUrlService.verifyEmailViaOtpCode(
                "Basic ${servicesApiKey.toBase64()}",
                LiveAccountApi.OtpBody(
                    email = email,
                    code = code
                )
            )
            if (res.isSuccessful) {
                val publishableKey = res.body()?.publishableKey
                publishableKey?.let { AbstractSuccess(it) }
                    ?: AbstractFailure(OtpError(Exception("no publishable key in response body")))
            } else {
                BackendException(res).let {
                    when (it.statusCode) {
                        "CodeMismatchException" -> {
                            OtpWrongCode
                        }
                        "NotAuthorizedException" -> {
                            if (it.message == "User cannot be confirmed. Current status is CONFIRMED") {
                                OtpSignInRequired
                            } else {
                                OtpError(it)
                            }
                        }
                        else -> {
                            OtpError(it)
                        }
                    }
                }.let { AbstractFailure(it) }
            }
        } catch (e: Exception) {
            AbstractFailure(OtpError(e))
        }
    }

}

sealed class OtpFailure
object OtpSignInRequired : OtpFailure()
object OtpWrongCode : OtpFailure()
class OtpError(val exception: Exception) : OtpFailure()
