package com.hypertrack.android.ui.screens.sign_in.use_case.exception

import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.utils.exception.BaseException
import com.hypertrack.logistics.android.github.R

class InvalidDeeplinkFormatException(link: String) : BaseException("Invalid url format: $link") {
    fun toTextError() = TextError(R.string.sign_in_deeplink_invalid_format)
}
