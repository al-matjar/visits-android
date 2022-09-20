package com.hypertrack.android.interactors.app

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.interactors.app.action.ClearGeofencesForMapAction
import com.hypertrack.android.interactors.app.action.GeofencesForMapLoadedAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.action.SignedInAction
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.interactors.app.optics.GeofencesForMapOptic
import com.hypertrack.android.interactors.app.reducer.DeeplinkReducer
import com.hypertrack.android.interactors.app.reducer.GeofencesForMapReducer
import com.hypertrack.android.interactors.app.reducer.HistoryReducer
import com.hypertrack.android.interactors.app.reducer.HistoryViewReducer
import com.hypertrack.android.interactors.app.reducer.login.LoginReducer
import com.hypertrack.android.interactors.app.reducer.ScreensReducer
import com.hypertrack.android.interactors.app.reducer.TimerReducer
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.NoneScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.interactors.app.state.allGeofences
import com.hypertrack.android.models.local.GeofenceForMap
import com.hypertrack.android.repository.access_token.AccountSuspendedException
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.asSet
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.state_machine.chain
import com.hypertrack.android.utils.state_machine.effectIf
import com.hypertrack.android.utils.state_machine.mergeResults
import com.hypertrack.android.utils.withEffects

class AppReducer(
    private val useCases: UseCases,
    private val appScope: AppScope,
    private val deeplinkReducer: DeeplinkReducer,
    private val loginReducer: LoginReducer,
    private val historyReducer: HistoryReducer,
    private val historyViewReducer: HistoryViewReducer,
    private val screensReducer: ScreensReducer,
    private val geofencesForMapReducer: GeofencesForMapReducer,
    private val timerReducer: TimerReducer
) {

    fun reduce(state: AppState, action: AppAction): ReducerResult<out AppState, out AppEffect> {
        return when (state) {
            is AppNotInitialized -> {
                when (action) {
                    is InitAppAction -> {
                        state.withEffects(InitAppEffect(state.appScope))
                    }
                    is AppInitializedAction -> {
                        // app is initialized
                        val initialized = AppInitialized(
                            state.appScope,
                            useCases,
                            action.userState,
                            tripCreationScope = null,
                            userIsLoggingIn = null,
                            viewState = state.splashScreenViewState ?: NoneScreenView,
                            timerJobs = state.timerJobs
                        )
                        val pushEffect = getHandlePendingPushEffect(
                            action.userState,
                            state.pendingPushNotification
                        )
                        deeplinkReducer.reduce(action, state, initialized)
                            .withEffects {
                                it.effects + pushEffect
                            }
                    }
                    is AppErrorAction -> {
                        handleAction(action, state)
                    }
                    is DeeplinkCheckedAction -> {
                        deeplinkReducer.reduce(action, state)
                    }
                    is PushReceivedAction -> {
                        state.copy(pendingPushNotification = action.remoteMessage).withEffects()
                    }
                    is RegisterScreenAction -> {
                        screensReducer.reduce(action, state)
                    }
                    is DeeplinkAppAction -> {
                        deeplinkReducer.reduce(action.action, state)
                    }
                    is TimerAppAction -> {
                        timerReducer.reduce(action.action, state)
                    }
                    is AppEffectAction -> {
                        state.withEffects(action.appEffect)
                    }
                    is TrackingStateChangedAction,
                    is ActivityOnNewIntent,
                    is UserLocationChangedAction -> {
                        // do nothing
                        state.withEffects()
                    }
                    OnAccountSuspendedAction,
                    is GeofencesForMapAppAction,
                    is CreateTripCreationScopeAction,
                    DestroyTripCreationScopeAction,
                    is HistoryAppAction,
                    is HistoryViewAppAction,
                    is LoginAppAction -> {
                        illegalAction(action, state)
                    }
                }
            }
            is AppInitialized -> {
                when (action) {
                    is DeeplinkCheckedAction -> {
                        deeplinkReducer.reduce(action, state)
                    }
                    is PushReceivedAction -> {
                        handleAction(action, state, state.appScope, state.userState)
                    }
                    is InitAppAction, is AppInitializedAction -> {
                        illegalAction(action, state)
                    }
                    is TrackingStateChangedAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                state.copy(
                                    userState = state.userState.copy(
                                        trackingState = action.trackingState
                                    )
                                ).withEffects(
                                    effectIf(action.trackingState != state.userState.trackingState) {
                                        AppEventEffect(TrackingStateChangedEvent(action.trackingState))
                                    }
                                )
                            }
                            UserNotLoggedIn -> {
                                state.withEffects()
                            }
                        }
                    }
                    is CreateTripCreationScopeAction -> {
                        state.copy(tripCreationScope = TripCreationScope(action.destinationData))
                            .withEffects()
                    }
                    DestroyTripCreationScopeAction -> {
                        state.copy(tripCreationScope = null)
                            .withEffects()
                    }
                    is AppErrorAction -> {
                        handleAction(action, state)
                    }
                    is ActivityOnNewIntent -> {
                        if (action.intent != null && action.intent.data != null) {
                            state.copy(showProgressbar = true).withEffects()
                        } else {
                            state.withEffects()
                        }
                    }
                    is HistoryAppAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                historyReducer.reduce(
                                    action,
                                    state.userState,
                                    AppStateOptics.getHistorySubState(
                                        state.userState,
                                        state.viewState
                                    )
                                ).withState { newHistoryState ->
                                    AppStateOptics.putHistorySubState(
                                        state,
                                        state.userState,
                                        newHistoryState
                                    )
                                }
                            }
                            UserNotLoggedIn -> {
                                illegalAction(action, state)
                            }
                        }
                    }
                    is UserLocationChangedAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                val historyViewEffects =
                                    AppStateOptics.getHistoryViewState(state)?.let {
                                        historyViewReducer.getEffects(
                                            action,
                                            state.userState.history,
                                            it
                                        )
                                    } ?: setOf()

                                state.copy(
                                    userState = state.userState.copy(
                                        userLocation = action.userLocation,
                                    )
                                ).withEffects(
                                    setOf(
                                        AppEventEffect(UpdateUserLocationEvent(action.userLocation))
                                    ) + historyViewEffects
                                )
                            }
                            UserNotLoggedIn -> {
                                state.withEffects()
                            }
                        }
                    }
                    is RegisterScreenAction -> {
                        screensReducer.reduce(action, state)
                    }
                    is HistoryViewAppAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                historyReducer.reduce(
                                    action,
                                    state.userState,
                                    AppStateOptics.getHistorySubState(
                                        state.userState,
                                        state.viewState
                                    )
                                ).withState { newHistoryState ->
                                    AppStateOptics.putHistorySubState(
                                        state,
                                        state.userState,
                                        newHistoryState
                                    )
                                }
                            }
                            UserNotLoggedIn -> {
                                illegalAction(action, state)
                            }
                        }
                    }
                    is GeofencesForMapAppAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                geofencesForMapReducer.reduce(
                                    action.action,
                                    state.userState.geofencesForMap.tiles,
                                    state.userState.useCases
                                ).withEffects { result ->
                                    val geofencesForMapEffects = result.effects
                                    val appEventEffect = when (action.action) {
                                        ClearGeofencesForMapAction, is GeofencesForMapLoadedAction -> {
                                            AppEventEffect(
                                                GeofencesForMapUpdatedEvent(
                                                    result.newState.allGeofences().map {
                                                        GeofenceForMap.fromGeofence(it)
                                                    }
                                                )
                                            ).asSet()
                                        }
                                        is LoadGeofencesForMapAction -> {
                                            geofencesForMapEffects
                                        }
                                    }
                                    geofencesForMapEffects + appEventEffect
                                }.withState { newGeofencesState ->
                                    GeofencesForMapOptic.set(
                                        state,
                                        state.userState,
                                        newGeofencesState
                                    )
                                }
                            }
                            UserNotLoggedIn -> {
                                state.withEffects()
                            }
                        }
                    }
                    OnAccountSuspendedAction -> {
                        when (state.userState) {
                            is UserLoggedIn -> {
                                state.copy(userState = UserNotLoggedIn).withEffects(
                                    ShowAndReportAppErrorEffect(AccountSuspendedException()),
                                    DestroyUserScopeEffect(state.userState.userScope)
                                )
                            }
                            UserNotLoggedIn -> {
                                illegalAction(action, state)
                            }
                        }
                    }
                    is TimerAppAction -> {
                        timerReducer.reduce(action.action, state)
                    }
                    is DeeplinkAppAction -> {
                        deeplinkReducer.reduce(action.action, state)
                    }
                    is AppEffectAction -> {
                        state.withEffects(action.appEffect)
                    }
                    is LoginAppAction -> {
                        action.action.let { loginAction ->
                            if (loginAction is SignedInAction) {
                                chain(
                                    loginReducer.reduce(loginAction, state)
                                ) {
                                    historyReducer.reduce(loginAction, it)
                                }
                            } else {
                                loginReducer.reduce(loginAction, state)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAction(action: AppErrorAction, state: AppState)
            : ReducerResult<out AppState, out AppEffect> {
        return state.withEffects(
            ShowAndReportAppErrorEffect(action.exception)
        )
    }

    private fun handleAction(
        action: PushReceivedAction,
        state: AppState,
        appScope: AppScope,
        userState: UserState
    ): ReducerResult<AppState, AppEffect> {
        val effects = when (userState) {
            is UserLoggedIn -> {
                setOf(
                    HandlePushEffect(
                        userState,
                        appScope.appContext,
                        action.remoteMessage
                    )
                )
            }
            is UserNotLoggedIn -> {
                setOf()
            }
        }
        return state.withEffects(effects)
    }

    private fun illegalAction(
        action: AppAction,
        state: AppState
    ): ReducerResult<out AppState, out AppEffect> {
        return IllegalActionException(action, state).let {
            if (MyApplication.DEBUG_MODE) {
                throw it
            } else {
                state.withEffects(
                    ShowAndReportAppErrorEffect(it)
                )
            }
        }
    }

    private fun getHandlePendingPushEffect(
        userState: UserState,
        pendingPushNotification: RemoteMessage?
    ): Set<AppEffect> {
        return when (userState) {
            is UserLoggedIn -> {
                if (pendingPushNotification != null) {
                    setOf(
                        HandlePushEffect(
                            userState,
                            appScope.appContext,
                            pendingPushNotification
                        )
                    )
                } else {
                    setOf()
                }
            }
            is UserNotLoggedIn -> {
                setOf()
            }
        }
    }


}
