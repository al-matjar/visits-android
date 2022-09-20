package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.HistoryViewAppAction
import com.hypertrack.android.interactors.app.LoadHistoryEffect
import com.hypertrack.android.interactors.app.ReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.action.DayHistoryErrorAction
import com.hypertrack.android.interactors.app.action.DayHistoryLoadedAction
import com.hypertrack.android.interactors.app.action.HistoryAction
import com.hypertrack.android.interactors.app.action.RefreshSummaryAction
import com.hypertrack.android.interactors.app.action.SignedInAction
import com.hypertrack.android.interactors.app.action.StartDayHistoryLoadingAction
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryScreenState
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ViewReadyAction
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OnResumeAction
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.mergeResults
import com.hypertrack.android.utils.withEffects
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HistoryReducer(
    private val appScope: AppScope,
    private val historyViewReducer: HistoryViewReducer
) {

    fun reduce(
        historyAction: HistoryAppAction,
        userState: UserLoggedIn,
        oldHistorySubState: HistorySubState
    ): ReducerResult<out HistorySubState, out AppEffect> {
        val action = historyAction.historyAction
        val newHistoryStateResult = reduce(action, oldHistorySubState.history, userState.userScope)
        return mergeResults(
            newHistoryStateResult,
            otherResult = { newHistoryState: ReducerResult<out HistoryState, out AppEffect> ->
                if (oldHistorySubState.historyScreenState != null) {
                    historyViewReducer.map(
                        userState,
                        newHistoryState.newState,
                        oldHistorySubState.historyScreenState
                    ).toNullable()
                    ReducerResult<HistoryScreenState?, AppEffect>(null, setOf())
                } else {
                    ReducerResult<HistoryScreenState?, AppEffect>(null, setOf())
                }
            }
        ) { newHistoryState: HistoryState, newViewState: HistoryScreenState? ->
            HistorySubState(history = newHistoryState, historyScreenState = newViewState)
        }
    }

    fun reduce(
        historyViewAction: HistoryViewAppAction,
        userState: UserLoggedIn,
        historySubState: HistorySubState
    ): ReducerResult<HistorySubState, out AppEffect> {
        return if (historySubState.historyScreenState == null) {
            // not on History screen
            return historySubState.withEffects(
                when (historyViewAction.historyViewAction) {
                    is ViewReadyAction,
                    OnResumeAction -> {
                        // this actions can be sent because viewpager instantiates neighbour tabs
                        setOf()
                    }
                    else -> {
                        setOf(
                            ShowAndReportAppErrorEffect(
                                IllegalActionException(
                                    historyViewAction,
                                    historySubState
                                )
                            )
                        )
                    }
                }
            )
        } else {
            // on History screen
            historyViewReducer.reduce(
                historyViewAction.historyViewAction,
                historySubState.historyScreenState,
                historySubState.history,
                userState
            ).withState {
                historySubState.copy(historyScreenState = it)
            }
        }
    }

    private fun reduce(
        action: HistoryAction,
        oldState: HistoryState,
        userScope: UserScope
    ): ReducerResult<out HistoryState, out AppEffect> {
        return when (action) {
            is StartDayHistoryLoadingAction -> {
                val shouldLoad = if (action.day == LocalDate.now()) {
                    val timeoutPassed = ChronoUnit.MILLIS.between(
                        oldState.lastTodayReload,
                        ZonedDateTime.now()
                    ) > HISTORY_RELOAD_TIMEOUT || action.forceReloadIfTimeout

                    timeoutPassed && oldState.days[action.day].let { day ->
                        if (day != null) {
                            // should load if not already loading
                            action.forceReloadIfLoading || day !is Loading
                        } else {
                            // should load
                            true
                        }
                    }
                } else {
                    if (oldState.days.containsKey(action.day)) {
                        // should reload day in the past only if there was an error
                        oldState.days.getValue(action.day).let { day ->
                            day is LoadingFailure || (action.forceReloadIfLoading && day is Loading)
                        }
                    } else {
                        true
                    }
                }
                if (shouldLoad) {
                    oldState.withDay(action.day, Loading())
                        .withEffects(
                            LoadHistoryEffect(action.day, userScope) as AppEffect
                        )
                } else {
                    oldState.withEffects()
                }
            }
            is DayHistoryLoadedAction -> {
                oldState.withDay(action.day, LoadingSuccess(action.history))
                    .let {
                        if (action.day == LocalDate.now()) {
                            it.copy(lastTodayReload = ZonedDateTime.now())
                        } else it
                    }
                    .withEffects()
            }
            is DayHistoryErrorAction -> {
                oldState.withDay(action.day, LoadingFailure(action.errorMessage)).withEffects(
                    ReportAppErrorEffect(action.exception)
                )
            }
            RefreshSummaryAction -> {
                reduce(
                    StartDayHistoryLoadingAction(LocalDate.now(), forceReloadIfLoading = true),
                    oldState,
                    userScope
                )
            }
        }
    }

    fun reduce(
        action: SignedInAction,
        state: AppInitialized
    ): ReducerResult<AppInitialized, AppEffect> {
        return state.withEffects(
            AppActionEffect(
                HistoryAppAction(
                    StartDayHistoryLoadingAction(
                        AppStateOptics.getHistoryViewState(state)?.selectedDay ?: LocalDate.now()
                    )
                )
            )
        )
    }

    companion object {
        const val HISTORY_RELOAD_TIMEOUT = 30 * 1000
    }

}

