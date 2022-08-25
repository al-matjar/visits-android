package com.hypertrack.android.utils.state_machine

open class ReducerResult<State1, Effect>(val newState: State1, val effects: Set<Effect>) {
    constructor(newState: State1) : this(newState, setOf())

    fun withAdditionalEffects(vararg effects: Effect): ReducerResult<State1, Effect> {
        return ReducerResult(newState, this.effects + effects.toSet())
    }

    fun withAdditionalEffects(effects: Set<Effect>): ReducerResult<State1, Effect> {
        return ReducerResult(newState, this.effects + effects)
    }

    fun withAdditionalEffects(effects: (State1) -> Set<Effect>): ReducerResult<State1, Effect> {
        return ReducerResult(newState, this.effects + effects.invoke(this.newState))
    }

    fun <N> withState(state: (State1) -> N): ReducerResult<N, out Effect> {
        return ReducerResult(state.invoke(this.newState), this.effects)
    }

    fun <NE> withEffects(state: (ReducerResult<State1, Effect>) -> Set<NE>): ReducerResult<State1, NE> {
        return ReducerResult(this.newState, state.invoke(this))
    }

    fun toNullable(): ReducerResult<State1?, out Effect> {
        return this.withState { it as State1? }
    }

    // merge two results with different states into one
    fun <State2, MergedState> mergeResult(
        // get other result to merge
        otherReducer: (State1) -> ReducerResult<State2, out Effect>,
        mergeFunction: (state1: State1, state2: State2) -> MergedState
    ): ReducerResult<MergedState, out Effect> {
        val result = otherReducer.invoke(newState)
        return ReducerResult(
            mergeFunction.invoke(newState, result.newState),
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

fun <T, E> effectIfNotNull(value: E?, effects: (value: E) -> T): Set<T> {
    return if (value != null) {
        setOf(effects.invoke(value))
    } else {
        setOf()
    }
}
