package com.hypertrack.android.utils

//todo split to different files
sealed class SimpleResult
object JustSuccess : SimpleResult()
class JustFailure(val exception: Exception) : SimpleResult() {
    fun <T> toFailure(): Failure<T> {
        return Failure(exception)
    }
}

sealed class Result<T> {
    fun <E> map(mapFunction: (T) -> E): Result<E> {
        return when (this) {
            is Success -> Success(mapFunction(this.data))
            is Failure -> Failure(this.exception)
        }
    }

    fun <E> flatMap(mapFunction: (T) -> Result<E>): Result<E> {
        return when (this) {
            is Success -> mapFunction(this.data)
            is Failure -> Failure(this.exception)
        }
    }

    fun flatMapSimple(mapFunction: (T) -> SimpleResult): SimpleResult {
        return when (this) {
            is Success -> mapFunction(this.data).let { JustSuccess }
            is Failure -> JustFailure(this.exception)
        }
    }
}

fun <T> Failure<T>.toSimple(): JustFailure {
    return JustFailure(this.exception)
}

class Success<T>(val data: T) : Result<T>()
class Failure<T>(val exception: Exception) : Result<T>()

fun <T> T.asSuccess(): Result<T> {
    return Success(this)
}

fun <T> Exception.asFailure(): Result<T> {
    return Failure(this)
}

fun Exception.asSimpleFailure(): JustFailure {
    return JustFailure(this)
}

fun <T> Result<T>.toNullable(): T? {
    return if (this is Success) {
        data
    } else {
        null
    }
}

fun <T> Result<T>.toNullableWithErrorReporting(
    crashReportsProvider: CrashReportsProvider
): T? {
    return when (this) {
        is Success -> {
            data
        }
        is Failure -> {
            crashReportsProvider.logException(exception)
            null
        }
    }
}

//todo rename
sealed class AbstractResult<S, F>
class AbstractSuccess<S, F>(val success: S) : AbstractResult<S, F>()
class AbstractFailure<S, F>(val failure: F) : AbstractResult<S, F>()


