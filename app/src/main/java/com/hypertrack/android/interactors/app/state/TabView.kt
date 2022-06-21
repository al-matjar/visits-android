package com.hypertrack.android.interactors.app.state

import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState

sealed class TabView {
    override fun toString(): String = javaClass.simpleName
}
object CurrentTripTab : TabView()
data class HistoryTab(
    val historyScreenState: HistoryScreenState
) : TabView()

object OrdersTab : TabView()
object PlacesTab : TabView()
object SummaryTab : TabView()
object ProfileTab : TabView()

