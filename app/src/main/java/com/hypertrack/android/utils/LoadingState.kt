package com.hypertrack.android.utils

sealed class LoadingState<T> {
    override fun toString(): String = javaClass.simpleName

    fun <E> map(mapFunction: (T) -> E): LoadingState<E> {
        return when (this) {
            is LoadingSuccess -> LoadingSuccess(mapFunction(this.data))
            is Loading -> Loading()
            is LoadingFailure -> LoadingFailure(this.exception)
        }
    }
}

data class LoadingSuccess<T>(val data: T) : LoadingState<T>()
class Loading<T>() : LoadingState<T>() {
    override fun toString(): String = javaClass.simpleName
}

data class LoadingFailure<T>(val exception: Exception) : LoadingState<T>()
