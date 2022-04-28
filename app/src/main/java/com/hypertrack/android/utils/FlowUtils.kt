@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.hypertrack.android.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf

fun <T> T.toFlow(): Flow<T> {
    return flowOf(this)
}

fun <T> Flow<T>.catchException(action: suspend (Exception) -> Unit): Flow<T> {
    return catch { e ->
        if (e is Exception) {
            action.invoke(e)
        } else throw e
    }
}

fun <T, E> Flow<Result<T>>.flatMapSuccess(action: suspend (T) -> Flow<Result<E>>): Flow<Result<E>> {
    return flatMapConcat {
        when (it) {
            is Success -> action.invoke(it.data)
            is Failure -> Failure<E>(it.exception).toFlow()
        }
    }
}

fun <T> Flow<Result<T>>.flatMapSimpleSuccess(action: suspend (T) -> Flow<SimpleResult>): Flow<SimpleResult> {
    return flatMapConcat {
        when (it) {
            is Success -> action.invoke(it.data)
            is Failure -> JustFailure(it.exception).toFlow()
        }
    }
}
