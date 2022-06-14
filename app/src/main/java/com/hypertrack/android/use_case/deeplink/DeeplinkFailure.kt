package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.ui.common.use_case.get_error_message.UnknownError
import com.hypertrack.android.ui.screens.sign_in.use_case.InvalidDeeplinkFormat
import com.hypertrack.logistics.android.github.R

sealed class DeeplinkFailure {
    override fun toString(): String = javaClass.simpleName

    fun toException(): Exception {
        return InvalidDeeplinkException(this)
    }

    fun toTextError(): TextError {
        val failure = this
        return when (failure) {
            is DeeplinkException -> {
                when (failure.exception) {
                    is InvalidDeeplinkFormat -> {
                        TextError(R.string.sign_in_deeplink_invalid_format)
                    }
                    else -> {
                        UnknownError
                    }
                }
            }
            NoPublishableKey -> {
                TextError(R.string.splash_screen_no_key)
            }
            NoLogin -> {
                TextError(R.string.splash_screen_no_username)
            }
            MirroredFieldsInMetadata -> {
                TextError(R.string.splash_screen_duplicate_fields)
            }
            DeprecatedDeeplink -> {
                TextError(R.string.splash_screen_deprecated_deeplink)
            }
        }
    }
}

data class DeeplinkException(val exception: Exception) : DeeplinkFailure()
object DeprecatedDeeplink : DeeplinkFailure()
object MirroredFieldsInMetadata : DeeplinkFailure()
object NoLogin : DeeplinkFailure()
object NoPublishableKey : DeeplinkFailure()
