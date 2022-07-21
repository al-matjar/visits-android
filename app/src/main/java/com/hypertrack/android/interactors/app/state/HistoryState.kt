package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.models.local.History
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.LoadingSuccess
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
        val daysString = days.entries.map {
            "(${it.key}: ${it.value})"
        }
        return "${javaClass.simpleName}(days=$daysString, lastTodayReload=$lastTodayReload)"
    }
}
