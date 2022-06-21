package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.models.local.History
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.LoadingState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryStateTest {
    companion object {
        fun historyState(
            days: Map<LocalDate, LoadingState<History, ErrorMessage>> = mapOf(),
            lastTodayReload: ZonedDateTime? = null
        ): HistoryState {
            return HistoryState(
                lastTodayReload = lastTodayReload ?: ZonedDateTime.ofInstant(
                    Instant.EPOCH,
                    ZoneId.systemDefault()
                ),
                days = days
            )
        }
    }
}
