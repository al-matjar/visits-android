package com.hypertrack.android.utils.message

import android.content.Context
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.logistics.android.github.R

sealed class AppMessage {
    abstract fun construct(resourceProvider: ResourceProvider): Result<String>
}

data class LoggedInMessage(val userData: UserData) : AppMessage() {
    override fun construct(resourceProvider: ResourceProvider): Result<String> {
        return when {
            userData.email != null -> {
                resourceProvider.stringFromResource(
                    R.string.logged_in,
                    userData.email.value
                ).asSuccess()
            }
            userData.phone != null -> {
                resourceProvider.stringFromResource(
                    R.string.logged_in,
                    userData.phone.value
                ).asSuccess()
            }
            else -> {
                NullPointerException().asFailure()
            }
        }
    }
}
