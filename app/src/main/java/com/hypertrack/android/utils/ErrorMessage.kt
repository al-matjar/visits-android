package com.hypertrack.android.utils

import java.lang.Exception


data class ErrorMessage(val text: String) {
    constructor(hint: String, text: String, appendNewLine: Boolean = true) :
            this(
                "${
                    hint
                }:${
                    if (appendNewLine) "\n" else " "
                }$text"
            )

    constructor(exception: Exception) : this(exception.format())
}
