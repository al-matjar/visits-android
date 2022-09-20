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

data class Success<T>(val data: T) : Result<T>()
data class Failure<T>(val exception: Exception) : Result<T>()

fun <T> T.asSuccess(): Result<T> {
    return Success(this)
}

fun <T> Exception.asFailure(): Result<T> {
    return Failure(this)
}

fun Exception.asSimpleFailure(): JustFailure {
    return JustFailure(this)
}

fun Any.asSimpleSuccess(): JustSuccess {
    return JustSuccess
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
sealed class AbstractResult<S, F> {
    fun <N> map(mapFunction: (S) -> N): AbstractResult<N, F> {
        return when (this) {
            is AbstractSuccess -> AbstractSuccess(mapFunction(this.success))
            is AbstractFailure -> AbstractFailure(this.failure)
        }
    }
}

data class AbstractSuccess<S, F>(val success: S) : AbstractResult<S, F>()
data class AbstractFailure<S, F>(val failure: F) : AbstractResult<S, F>()


