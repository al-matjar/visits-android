package com.hypertrack.android.utils

import com.hypertrack.android.utils.state_machine.ReducerResultWithViewStateEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

//todo separate file
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

    fun <NE> withEffects(state: (Set<E>) -> Set<NE>): ReducerResult<S, NE> {
        return ReducerResult(this.newState, state.invoke(effects))
    }

    fun toNullable(): ReducerResult<S?, out E> {
        return this.withState { it as S? }
    }

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


//todo dedicated package
open class StateMachine<A, S, E>(
    private val tag: String,
    private val crashReportsProvider: CrashReportsProvider,
    initialState: S,
    private val scope: CoroutineScope,
    private val context: CoroutineContext,
    private val reduce: (state: S, action: A) -> ReducerResult<out S, out E>,
    private val applyEffects: (effects: Set<E>) -> Unit,
    // effects that are need to be executed to move to certain state
    // in addition to ones emitted by reduce()
    private val stateChangeEffects: (newState: S) -> Set<E> = { setOf() }
) {
    private var _state: S = initialState
    val state: S
        get() = _state

    fun handleAction(action: A) {
        scope.launch(context) {
            reduce(_state, action).let {
                _state = it.newState
                val effects = stateChangeEffects.invoke(it.newState) + it.effects
                applyEffects(effects)
                if (shouldLog(action, it.newState, effects)) {
                    log(format(action, it.newState, effects))
                }
            }
        }
    }

    private fun format(action: A, newState: S, effects: Set<E>): String {
        val msg = StringBuilder()
        msg.appendLine(tag)
        msg.appendLine("v ${getActionString(action)}")
        msg.appendLine("= ${getStateString(newState)}")
        msg.appendLine("effects: [")
        effects.forEach { effect ->
            msg.appendLine("\t> ${getEffectString(effect)}")
        }
        msg.appendLine("]")
        return msg.toString()
    }

    protected open fun shouldLog(action: A, newState: S, effects: Set<E>): Boolean {
        return true
    }

    protected open fun getStateString(state: S): String {
        return state.toString()
    }

    protected open fun getActionString(action: A): String {
        return action.toString()
    }

    protected open fun getEffectString(effect: E): String {
        return effect.toString()
    }

    protected open fun log(txt: String) {
        crashReportsProvider.log(txt)
    }
}

fun <S : Any, E : Any> S.withEffects(vararg effects: E): ReducerResult<S, E> {
    return ReducerResult(
        this,
        effects.toMutableSet()
    )
}

fun <S : Any, E : Any> S.withEffects(effects: Set<E>): ReducerResult<S, E> {
    return ReducerResult(
        this,
        effects
    )
}

fun <S, E, VE : E> ReducerResult<S, E>.withViewEffect(
    viewEffect: (S) -> VE
): ReducerResultWithViewStateEffect<S, E, VE> {
    return ReducerResultWithViewStateEffect(
        newState,
        effects,
        viewEffect.invoke(newState)
    )
}
