package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.logistics.android.github.R

class InvalidDeeplinkFormat(link: String) : Exception("Invalid url format: $link") {
    fun toTextError() = TextError(R.string.sign_in_deeplink_invalid_format)
}
