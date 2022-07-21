package com.hypertrack.android.interactors.app.optics

import com.hypertrack.android.interactors.app.reducer.HistorySubState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.interactors.app.state.HistoryTab
import com.hypertrack.android.interactors.app.state.TabsView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState

object AppStateOptics {

    //todo move history-related methods to HistoryOptic
    fun getHistoryViewState(state: AppState): HistoryScreenState? {
        return when (state) {
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        when (state.viewState) {
                            is TabsView -> {
                                state.viewState.historyTab?.historyScreenState
                            }
                            else -> null
                        }
                    }
                    UserNotLoggedIn -> null
                }
            }
            is AppNotInitialized -> null
        }
    }

    fun getHistoryState(state: AppState): HistoryState? {
        return when (state) {
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState.history
                    }
                    UserNotLoggedIn -> null
                }
            }
            is AppNotInitialized -> null
        }
    }

    fun getHistorySubState(userState: UserLoggedIn, viewState: AppViewState): HistorySubState {
        return HistorySubState(
            userState.history,
            if (viewState is TabsView) {
                viewState.historyTab?.historyScreenState
            } else null
        )
    }

    fun putHistorySubState(
        state: AppInitialized,
        userState: UserLoggedIn,
        newHistoryState: HistorySubState
    ): AppState {
        return state.copy(
            userState = userState.copy(
                history = newHistoryState.history
            ),
            viewState = if (
                newHistoryState.historyScreenState != null &&
                state.viewState is TabsView
            ) {
                state.viewState.copy(
                    historyTab = HistoryTab(newHistoryState.historyScreenState)
                )
            } else {
                state.viewState
            }
        )
    }

    // todo reuse in pther places
    fun getUserLoggedIn(state: AppState): UserLoggedIn? {
        return when (state) {
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState
                    }
                    UserNotLoggedIn -> null
                }
            }
            is AppNotInitialized -> null
        }
    }
}

