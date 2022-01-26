package com.hypertrack.android.interactors.history

import com.hypertrack.android.models.local.LocalHistory
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.ReducerResult
import java.time.LocalDate

data class HistoryState(
    val days: Map<LocalDate, LoadingState<LocalHistory>>
) {
    fun withDay(date: LocalDate, dayState: LoadingState<LocalHistory>): HistoryState {
        return copy(days = days.toMutableMap().apply { put(date, dayState) })
    }
}

fun HistoryState.asReducerResult(): ReducerResult<HistoryState, Effect> {
    return ReducerResult(this)
}

fun HistoryState.withEffects(effects: Set<Effect>): ReducerResult<HistoryState, Effect> {
    return ReducerResult(this, effects)
}

fun HistoryState.withEffects(vararg effect: Effect): ReducerResult<HistoryState, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}
