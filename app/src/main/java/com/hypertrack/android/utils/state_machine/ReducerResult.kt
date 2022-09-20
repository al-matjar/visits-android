package com.hypertrack.android.utils.state_machine

open class ReducerResult<State1, Effect>(val newState: State1, val effects: Set<Effect>) {
    constructor(newState: State1) : this(newState, setOf())

    fun <N> withState(state: (State1) -> N): ReducerResult<N, out Effect> {
        return ReducerResult(state.invoke(this.newState), this.effects)
    }

    fun <NE> withEffects(state: (ReducerResult<State1, Effect>) -> Set<NE>): ReducerResult<State1, NE> {
        return ReducerResult(this.newState, state.invoke(this))
    }

    fun toNullable(): ReducerResult<State1?, out Effect> {
        return this.withState { it as State1? }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(newState=$newState, effects=$effects)"
    }
}

fun <State, Effect> chain(
    firstResult: ReducerResult<out State, out Effect>,
    secondResult: (State) -> ReducerResult<out State, out Effect>,
): ReducerResult<out State, out Effect> {
    return mergeResults(
        firstResult,
        {
            secondResult.invoke(it.newState)
        }
    ) { _, state2 ->
        state2
    }
}

// merge two results with different states into one
fun <State1, State2, MergedState, Effect> mergeResults(
    firstResult: ReducerResult<out State1, out Effect>,
    otherResult: (ReducerResult<out State1, out Effect>) -> ReducerResult<out State2, out Effect>,
    mergeFunction: (state1: State1, state2: State2) -> MergedState
): ReducerResult<out MergedState, out Effect> {
    val result = otherResult.invoke(firstResult)
    return ReducerResult(
        mergeFunction.invoke(firstResult.newState, result.newState),
        firstResult.effects + result.effects
    )
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

fun <T, E> effectIfNotNull(value: E?, effects: (value: E) -> T): Set<T> {
    return if (value != null) {
        setOf(effects.invoke(value))
    } else {
        setOf()
    }
}
