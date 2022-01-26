package com.hypertrack.android.utils

import android.util.Log
import com.hypertrack.android.interactors.history.Action
import com.hypertrack.logistics.android.github.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.Executors.newSingleThreadExecutor
import kotlin.coroutines.CoroutineContext

open class ReducerResult<S, E>(val newState: S, val effects: Set<E>) {
    constructor(newState: S) : this(newState, setOf())
}

class StateMachine<A, S, E>(
    private val tag: String,
    initialState: S,
    private val scope: CoroutineScope,
    private val context: CoroutineContext,
    private val reduce: (state: S, action: A) -> ReducerResult<S, E>,
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
                if (BuildConfig.DEBUG) {
                    val msg = StringBuilder()
                    msg.appendLine(tag)
                    msg.appendLine("v $action")
                    msg.appendLine(it.newState.toString())
                    msg.appendLine("effects: [")
                    effects.forEach { effect ->
                        msg.appendLine("\t> $effect")
                    }
                    msg.appendLine("]")
                    Log.v("hypertrack-verbose", msg.toString())
                }
            }
        }
    }
}
