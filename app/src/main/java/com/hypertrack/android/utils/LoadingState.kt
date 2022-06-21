package com.hypertrack.android.utils

sealed class LoadingState<S, F> {
    override fun toString(): String = javaClass.simpleName

    fun <NS> mapSuccess(mapFunction: (S) -> NS): LoadingState<NS, F> {
        return when (this) {
            is LoadingSuccess -> LoadingSuccess(mapFunction(this.data))
            is Loading -> Loading()
            is LoadingFailure -> LoadingFailure(this.failure)
        }
    }
}

data class LoadingSuccess<S, F>(val data: S) : LoadingState<S, F>()

@Suppress("EqualsOrHashCode")
class Loading<S, F> : LoadingState<S, F>() {
    override fun toString(): String = javaClass.simpleName

    override fun equals(other: Any?): Boolean {
        return other is Loading<*, *>
    }
}

data class LoadingFailure<S, F>(val failure: F) : LoadingState<S, F>()
