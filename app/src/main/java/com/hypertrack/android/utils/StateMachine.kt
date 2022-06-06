package com.hypertrack.android.utils

import android.util.Log
import com.hypertrack.android.interactors.history.Action
import com.hypertrack.logistics.android.github.BuildConfig
import io.branch.referral.Branch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.Executors.newSingleThreadExecutor
import kotlin.coroutines.CoroutineContext

//todo separate file
data class ReducerResult<S, E>(val newState: S, val effects: Set<E>) {
    constructor(newState: S) : this(newState, setOf())

    fun withEffects(effects: Set<E>): ReducerResult<S, E> {
        return ReducerResult(newState, this.effects + effects)
    }
}


//todo dedicated package
class StateMachine<A, S, E>(
    private val tag: String,
    private val crashReportsProvider: CrashReportsProvider,
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
                log(format(action, it.newState, effects))
            }
        }
    }

    private fun format(action: A, newState: S, effects: Set<E>): String {
        val msg = StringBuilder()
        msg.appendLine(tag)
        msg.appendLine("v $action")
        msg.appendLine("= $newState")
        msg.appendLine("effects: [")
        effects.forEach { effect ->
            msg.appendLine("\t> $effect")
        }
        msg.appendLine("]")
        return msg.toString()
    }

    private fun log(txt: String) {
        crashReportsProvider.log(txt)
    }
}
