package com.hypertrack.android.ui.screens.sign_in.use_case

import androidx.annotation.StringRes
import com.hypertrack.android.use_case.deeplink.DeeplinkException
import com.hypertrack.android.use_case.deeplink.DeeplinkFailure
import com.hypertrack.android.use_case.deeplink.DeprecatedDeeplink
import com.hypertrack.android.use_case.deeplink.MultipleLogins
import com.hypertrack.android.use_case.deeplink.NoLogin
import com.hypertrack.android.use_case.deeplink.NoPublishableKey
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.SimpleException
import com.hypertrack.android.utils.asSimpleFailure
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.Flow

class HandleDeeplinkFailureUseCase(
    private val resourceProvider: ResourceProvider
) {

    // todo use in app deeplink error events
    fun execute(failure: DeeplinkFailure): Flow<JustFailure> {
        return when (failure) {
            is DeeplinkException -> {
                when (failure.exception) {
                    is InvalidDeeplinkFormat -> {
                        simpleException(R.string.sign_in_deeplink_invalid_format)
                    }
                    else -> {
                        failure.exception
                    }
                }
            }
            NoPublishableKey -> {
                simpleException(R.string.splash_screen_no_key)
            }
            NoLogin -> {
                simpleException(R.string.splash_screen_no_username)
            }
            MultipleLogins -> {
                simpleException(R.string.splash_screen_duplicate_fields)
            }
            DeprecatedDeeplink -> {
                simpleException(R.string.splash_screen_duplicate_fields)
            }
        }.asSimpleFailure().toFlow()
    }

    private fun simpleException(@StringRes res: Int): SimpleException {
        return SimpleException(resourceProvider.stringFromResource(res))
    }

}
