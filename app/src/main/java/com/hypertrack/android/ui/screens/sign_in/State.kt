package com.hypertrack.android.ui.screens.sign_in

import com.hypertrack.android.utils.ReducerResult

data class State(
    val login: String,
    val password: String,
    val deeplinkIssuesDialog: DeeplinkIssuesDialogState
) {
    // to avoid showing password in logs
    override fun toString(): String {
        return "${javaClass.simpleName}(login=$login, deeplinkIssuesDialog=$deeplinkIssuesDialog)"
    }
}

fun State.withEffects(effects: Set<Effect>): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effects
    )
}

fun State.withEffects(vararg effect: Effect): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}
