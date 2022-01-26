package com.hypertrack.android.utils

sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()

    override fun toString(): String {
        val str = when (this) {
            is Left -> "left=${left}"
            is Right -> "right=${right}"
        }
        return "Either($str)"
    }
}
