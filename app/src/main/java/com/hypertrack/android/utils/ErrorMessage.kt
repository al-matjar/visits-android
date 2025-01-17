package com.hypertrack.android.utils

import com.hypertrack.android.ui.base.Consumable
import kotlin.Exception

// todo move to message package
data class ErrorMessage(val text: String, val originalException: Exception? = null)

fun String.toErrorMessage(originalException: Exception? = null): ErrorMessage {
    return ErrorMessage(this, originalException)
}
