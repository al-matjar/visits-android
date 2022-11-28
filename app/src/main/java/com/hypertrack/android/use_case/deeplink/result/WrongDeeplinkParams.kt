package com.hypertrack.android.use_case.deeplink.result

import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.ui.common.use_case.get_error_message.UnknownError
import com.hypertrack.android.ui.screens.sign_in.use_case.exception.InvalidDeeplinkFormatException
import com.hypertrack.android.use_case.deeplink.exception.InvalidDeeplinkParamsException
import com.hypertrack.logistics.android.github.R

sealed class WrongDeeplinkParams {
    override fun toString(): String = javaClass.simpleName

    fun toException(): Exception {
        return InvalidDeeplinkParamsException(this)
    }

    fun toTextError(): TextError {
        val failure = this
        return when (failure) {
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

object DeprecatedDeeplink : WrongDeeplinkParams()
object MirroredFieldsInMetadata : WrongDeeplinkParams()
object NoLogin : WrongDeeplinkParams()
object NoPublishableKey : WrongDeeplinkParams()
