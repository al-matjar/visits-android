package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.models.local.History
import com.hypertrack.android.utils.ErrorMessage
import java.time.LocalDate

sealed class HistoryAction {
    override fun toString(): String = javaClass.simpleName
}

data class StartDayHistoryLoadingAction(
    val day: LocalDate,
    val forceReloadIfLoading: Boolean = false
) : HistoryAction()

data class DayHistoryLoadedAction(val day: LocalDate, val history: History) : HistoryAction()
data class DayHistoryErrorAction(
    val day: LocalDate,
    val exception: Exception,
    val errorMessage: ErrorMessage
) : HistoryAction()

object RefreshSummaryAction : HistoryAction()

