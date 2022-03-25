package com.hypertrack.android.utils

sealed class Optional<T> {
    fun toNullable(): T? {
        return when (this) {
            is HasValue -> value
            is NoValue -> null
        }
    }
}

class HasValue<T>(val value: T) : Optional<T>()
class NoValue<T> : Optional<T>()
