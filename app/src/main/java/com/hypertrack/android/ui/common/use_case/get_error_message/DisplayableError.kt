package com.hypertrack.android.ui.common.use_case.get_error_message

import androidx.annotation.StringRes
import com.hypertrack.logistics.android.github.R
import kotlin.Exception

sealed class DisplayableError
open class TextError(@StringRes val stringResource: Int) : DisplayableError()

object UnknownError : TextError(R.string.error_unknown)
object NetworkError : TextError(R.string.error_network)
object ServerError : TextError(R.string.error_server)

@Suppress("ArrayInDataClass")
data class ComplexTextError(
    @StringRes val stringResource: Int,
    val params: Array<out Any>
) : DisplayableError()

data class ExceptionError(val exception: Exception) : DisplayableError()

fun Int.asError(): TextError {
    return TextError(this)
}

fun Exception.asError(): ExceptionError {
    return ExceptionError(this)
}
