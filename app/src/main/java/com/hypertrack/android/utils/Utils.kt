package com.hypertrack.android.utils

import java.time.ZonedDateTime

sealed class ResultValue<T>
class ResultSuccess<T>(val value: T) : ResultValue<T>()
class ResultError<T>(val exception: Exception) : ResultValue<T>()

sealed class ResultEmptyValue
class ResultEmptyError<T>(val exception: Exception) : ResultEmptyValue()
object ResultEmptySuccess : ResultEmptyValue()

fun <T> List<T>.applyAddAll(toAdd: List<T>): List<T> {
    return toMutableList().apply { addAll(toAdd) }
}

class IllegalActionException(action: Any, state: Any) :
    IllegalStateException("Illegal action $action for state $state")

sealed class AlgBoolean
object True : AlgBoolean() {
    override fun toString(): String {
        return javaClass.simpleName
    }
}

object False : AlgBoolean() {
    override fun toString(): String {
        return javaClass.simpleName
    }
}

open class TypeWrapper<T>(val value: T)
