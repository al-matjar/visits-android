package com.hypertrack.android.ui.common.use_case.get_error_message

import com.hypertrack.android.api.BackendException
import com.hypertrack.android.api.graphql.GraphQlException
import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.interactors.app.reducer.deeplink.DeeplinkTimeoutException
import com.hypertrack.android.interactors.app.reducer.login.AlreadyLoggedInException
import com.hypertrack.android.interactors.app.reducer.login.LoginAlreadyInProgressException
import com.hypertrack.android.interactors.trip.NotClockedInException
import com.hypertrack.android.repository.access_token.AccountSuspendedException
import com.hypertrack.android.ui.screens.sign_in.use_case.InvalidDeeplinkFormat
import com.hypertrack.android.use_case.deeplink.InvalidDeeplinkException
import com.hypertrack.android.use_case.geofences.AdjacentGeofencesCheckTimeoutException
import com.hypertrack.android.use_case.handle_push.UnknownPushNotificationException
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.exception.ManuallyTriggeredException
import com.hypertrack.android.utils.exception.SimpleException
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.isNetworkError
import com.hypertrack.android.utils.toErrorMessage
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.lang.Exception

class GetErrorMessageUseCase(
    private val resourceProvider: ResourceProvider,
) {

    private val showFullErrorMessages = MyApplication.DEBUG_MODE

    fun execute(error: DisplayableError): Flow<ErrorMessage> {
        return when (error) {
            is TextError -> {
                getTextErrorString(error).toErrorMessage()
            }
            is ComplexTextError -> {
                resourceProvider.stringFromResource(error.stringResource, *error.params)
                    .toErrorMessage()
            }
            is ExceptionError -> {
                getMessageForException(error.exception).toErrorMessage(error.exception)
            }
        }.toFlow()
    }

    private fun getTextErrorString(error: TextError): String {
        return resourceProvider.stringFromResource(error.stringResource)
    }

    private fun getMessageForException(exception: Exception): String {
        return if (showFullErrorMessages) {
            exception.format()
        } else {
            when {
                exception is HttpException -> {
                    getTextErrorString(ServerError)
                }
                exception is SimpleException -> {
                    exception.message ?: getTextErrorString(UnknownError)
                }
                exception is InvalidDeeplinkFormat -> {
                    exception.toTextError().let {
                        getTextErrorString(it)
                    }
                }
                exception is InvalidDeeplinkException -> {
                    exception.deeplinkFailure.toTextError().let {
                        getTextErrorString(it)
                    }
                }
                exception is BackendException -> {
                    getTextErrorString(ServerError)
                }
                exception is NotClockedInException -> {
                    getTextErrorString(TextError(R.string.order_not_clocked_in))
                }
                exception is AdjacentGeofencesCheckTimeoutException -> {
                    getTextErrorString(TextError(R.string.add_place_timeout))
                }
                exception is AccountSuspendedException -> {
                    getTextErrorString(TextError(R.string.error_account_suspended))
                }
                exception is GraphQlException -> {
                    getTextErrorString(ServerError)
                }
                exception is LoginAlreadyInProgressException -> {
                    getTextErrorString(TextError(R.string.error_login_already_in_progress))
                }
                exception is AlreadyLoggedInException -> {
                    getTextErrorString(TextError(R.string.error_login_already_in_progress))
                }
                exception is DeeplinkTimeoutException -> {
                    getTextErrorString(TextError(R.string.error_deeplink_timeout))
                }
                exception.isNetworkError() -> {
                    getTextErrorString(NetworkError)
                }
                (
                        exception is BranchErrorException ||
                                exception is ManuallyTriggeredException ||
                                exception is IllegalActionException ||
                                exception is UnknownPushNotificationException
                        ) -> {
                    getTextErrorString(UnknownError)
                }
                else -> {
                    getTextErrorString(UnknownError)
                }
            }
        }
    }

}
