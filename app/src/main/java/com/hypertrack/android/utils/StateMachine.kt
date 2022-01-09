package com.hypertrack.android.utils

import android.util.Log
import com.hypertrack.logistics.android.github.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class ReducerResult<S, E>(val newState: S, val effects: Set<E>) {
    constructor(newState: S) : this(newState, setOf())
}

class StateMachine<A, S, E>(
    private val tag: String,
    private val scope: CoroutineScope,
    initialState: S,
    private val reduce: (state: S, action: A) -> ReducerResult<S, E>,
    private val applyEffects: (effects: Set<E>) -> Unit,
    private val stateChangeEffects: (newState: S) -> Set<E> = { setOf() }
) {
    private var _state: S = initialState
    val state: S
        get() = _state

    fun handleAction(action: A) {
        scope.launch {
            reduce(_state, action).let {
                _state = it.newState
                val effects = stateChangeEffects.invoke(it.newState) + it.effects
                applyEffects(effects)
                if (BuildConfig.DEBUG) {
                    Log.v(
                        "hypertrack-verbose",
                        "$tag \n v $action \n ${it.newState} \n > $effects "
                    )
                }
            }
        }
    }
}
