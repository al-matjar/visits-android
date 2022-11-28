@file:Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")

package com.hypertrack.android.utils

import android.util.Log
import androidx.lifecycle.Transformations.map
import com.hypertrack.android.use_case.app.threading.ActionsScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

fun <T, E> Flow<Result<out T>>.flatMapSuccess(action: suspend (T) -> Flow<Result<out E>>): Flow<Result<out E>> {
    return flatMapConcat {
        when (it) {
            is Success -> action.invoke(it.data)
            is Failure -> Failure<E>(it.exception).toFlow()
        }
    }
}

fun <T, E> Flow<Result<out T>>.mapSuccess(action: suspend (T) -> E): Flow<Result<E>> {
    return map {
        when (it) {
            is Success -> action.invoke(it.data).asSuccess()
            is Failure -> Failure(it.exception)
        }
    }
}

fun <T> Flow<Result<T>>.mapFailure(action: suspend (Exception) -> Exception): Flow<Result<T>> {
    return map {
        when (it) {
            is Success -> it
            is Failure -> Failure(action.invoke(it.exception))
        }
    }
}

fun <T> Flow<Result<out T>>.flatMapSimpleSuccess(action: suspend (T) -> Flow<SimpleResult>): Flow<SimpleResult> {
    return flatMapConcat {
        when (it) {
            is Success -> action.invoke(it.data)
            is Failure -> JustFailure(it.exception).toFlow()
        }
    }
}

fun <T> Flow<SimpleResult>.mapSimpleToSuccess(action: suspend () -> T): Flow<Result<T>> {
    return flatMapConcat {
        when (it) {
            JustSuccess -> action.invoke().asSuccess()
            is JustFailure -> Failure(it.exception)
        }.toFlow()
    }
}

fun <S, F, M> Flow<AbstractResult<S, F>>.flatMapAbstractSuccess(action: suspend (S) -> Flow<AbstractResult<M, F>>)
        : Flow<AbstractResult<M, F>> {
    return flatMapConcat {
        when (it) {
            is AbstractSuccess -> action.invoke(it.success)
            is AbstractFailure -> AbstractFailure<M, F>(it.failure).toFlow()
        }
    }
}

fun <T> Flow<T>.log(tag: String): Flow<T> {
    return map {
        Log.v(tag, it.toString())
        it
    }
}

fun <T, K> StateFlow<T>.mapState(scope: ActionsScope, block: (T) -> K): StateFlow<K> {
    return map(block).stateIn(scope.value, SharingStarted.Lazily, block.invoke(value))
}

fun <T> tryAsFlow(block: () -> T): Flow<Result<T>> {
    return { tryAsResult { block.invoke() } }.asFlow()
}

fun <T> MutableSharedFlow<T>.emitAsFlow(item: T): Flow<Unit> {
    return suspend { emit(item) }.asFlow()
}

fun <T> MutableStateFlow<T>.emitAsFlow(item: T): Flow<Unit> {
    return suspend { emit(item) }.asFlow()
}
