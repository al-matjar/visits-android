package com.hypertrack.android.utils.state_machine

open class ReducerResult<S, E>(val newState: S, val effects: Set<E>) {
    constructor(newState: S) : this(newState, setOf())

    fun withAdditionalEffects(vararg effects: E): ReducerResult<S, E> {
        return ReducerResult(newState, this.effects + effects.toSet())
    }

    fun withAdditionalEffects(effects: Set<E>): ReducerResult<S, E> {
        return ReducerResult(newState, this.effects + effects)
    }

    fun withAdditionalEffects(effects: (S) -> Set<E>): ReducerResult<S, E> {
        return ReducerResult(newState, this.effects + effects.invoke(this.newState))
    }

    fun <N> withState(state: (S) -> N): ReducerResult<N, out E> {
        return ReducerResult(state.invoke(this.newState), this.effects)
    }

    fun <NE> withEffects(state: (ReducerResult<S, E>) -> Set<NE>): ReducerResult<S, NE> {
        return ReducerResult(this.newState, state.invoke(this))
    }

    fun toNullable(): ReducerResult<S?, out E> {
        return this.withState { it as S? }
    }

    // merge two results with different states into one
    fun <OS, NS> mergeResult(
        resultFunction: (S) -> ReducerResult<OS, out E>,
        merge: (state1: S, state2: OS) -> NS
    ): ReducerResult<NS, E> {
        val result = resultFunction.invoke(newState)
        return ReducerResult(
            merge.invoke(newState, result.newState),
            effects + result.effects
        )
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(newState=$newState, effects=$effects)"
    }
}

fun <T> effectsIf(condition: Boolean, effects: () -> Set<T>): Set<T> {
    return if (condition) {
        effects.invoke()
    } else {
        setOf()
    }
}

fun <T> effectIf(condition: Boolean, effects: () -> T): Set<T> {
    return if (condition) {
        setOf(effects.invoke())
    } else {
        setOf()
    }
}
