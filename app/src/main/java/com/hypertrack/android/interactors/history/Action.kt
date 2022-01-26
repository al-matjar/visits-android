package com.hypertrack.android.interactors.history

import com.hypertrack.android.models.History
import com.hypertrack.android.models.local.LocalHistory
import java.time.LocalDate

sealed class Action
data class StartDayHistoryLoading(val day: LocalDate) : Action()
data class DayHistoryLoadedAction(val day: LocalDate, val history: LocalHistory) : Action()
data class DayHistoryErrorAction(val day: LocalDate, val exception: Exception) : Action()
