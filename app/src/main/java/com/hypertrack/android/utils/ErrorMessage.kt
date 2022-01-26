package com.hypertrack.android.utils


data class ErrorMessage(val text: String) {
    constructor(hint: String, text: String, appendNewLine: Boolean = true) :
            this(
                "${
                    hint
                }:${
                    if (appendNewLine) "\n" else " "
                }$text"
            )
}
