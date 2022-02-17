package com.hypertrack.android.use_case.login

import com.hypertrack.android.api.BackendException
import com.hypertrack.android.api.LiveAccountApi
import com.hypertrack.android.utils.toBase64
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class ResendEmailConfirmationUseCase(
    private val liveAccountUrlService: LiveAccountApi,
    private val servicesApiKey: String,
) {

    fun execute(email: String): Flow<ResendResult> {
        return suspend {
            resendEmailConfirmation(email)
        }.asFlow()
    }

    private suspend fun resendEmailConfirmation(email: String): ResendResult {
        try {
            val res = liveAccountUrlService.resendOtpCode(
                "Basic ${servicesApiKey.toBase64()}",
                LiveAccountApi.ResendBody(email)
            )
            if (res.isSuccessful) {
                return ResendNoAction
            } else {
                BackendException(res).let {
                    return when (it.statusCode) {
                        "InvalidParameterException" -> {
                            if (it.message == "User is already confirmed.") {
                                ResendAlreadyConfirmed
                            } else {
                                ResendError(it)
                            }
                        }
                        else -> {
                            ResendError(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return ResendError(e)
        }
    }
}

sealed class ResendResult
object ResendNoAction : ResendResult()
object ResendAlreadyConfirmed : ResendResult()
class ResendError(val exception: Exception) : ResendResult()
