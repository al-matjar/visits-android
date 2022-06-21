package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.models.local.History
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.MyApplication
import java.time.LocalDate
import java.time.ZonedDateTime

data class HistoryState(
    val lastTodayReload: ZonedDateTime,
    val days: Map<LocalDate, LoadingState<History, ErrorMessage>>
) {
    fun withDay(date: LocalDate, dayState: LoadingState<History, ErrorMessage>): HistoryState {
        return copy(days = days.toMutableMap().apply { put(date, dayState) })
    }

    override fun toString(): String {
        return if (MyApplication.DEBUG_MODE) {
            "${javaClass.simpleName}(days=${days.entries.map { "(${it.key}: ${it.value.javaClass.simpleName})" }}, lastTodayReload=$lastTodayReload)"
        } else {
            super.toString()
        }
    }
}
