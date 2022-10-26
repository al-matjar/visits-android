package com.hypertrack.android.utils.message

import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.logistics.android.github.R

sealed class AppMessage {
    fun construct(resourceProvider: ResourceProvider): Result<String> {
        return tryAsResult { getMessageText(resourceProvider) }.let {
            when (it) {
                is Success -> it.data
                is Failure -> Failure(it.exception)
            }
        }
    }

    protected abstract fun getMessageText(resourceProvider: ResourceProvider): Result<String>
}

class LoggedInMessage(private val userData: UserData) : AppMessage() {
    override fun getMessageText(resourceProvider: ResourceProvider): Result<String> {
        return when {
            userData.email != null -> {
                resourceProvider.stringFromResource(
                    R.string.app_message_logged_in,
                    userData.email.value
                ).asSuccess()
            }
            userData.phone != null -> {
                resourceProvider.stringFromResource(
                    R.string.app_message_logged_in,
                    userData.phone.value
                ).asSuccess()
            }
            else -> {
                NullPointerException().asFailure()
            }
        }
    }
}

object ErrorReportedMessage : AppMessage() {
    override fun getMessageText(resourceProvider: ResourceProvider): Result<String> {
        return resourceProvider.stringFromResource(R.string.app_message_error_reported).asSuccess()
    }
}
