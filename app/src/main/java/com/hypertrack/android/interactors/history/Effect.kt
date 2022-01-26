package com.hypertrack.android.interactors.history

import java.time.LocalDate

sealed class Effect
data class UpdateHistoryStateEffect(val historyState: HistoryState) : Effect()
data class LoadHistoryEffect(val date: LocalDate) : Effect()
