package com.hypertrack.android.utils.exception

open class BaseException(message: String? = null) : Exception(message) {
    override fun toString(): String {
        return "${javaClass.simpleName}" + if (message != null) {
            "(${message})"
        } else ""
    }
}
