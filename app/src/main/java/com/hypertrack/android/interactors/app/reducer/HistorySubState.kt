package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState
import com.squareup.moshi.JsonClass

data class HistorySubState(
    val history: HistoryState,
    val historyScreenState: HistoryScreenState?
)
