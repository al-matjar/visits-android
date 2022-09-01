package com.hypertrack.android.interactors.app

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.Injector.crashReportsProvider
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.interactors.app.action.ClearGeofencesForMapAction
import com.hypertrack.android.interactors.app.action.GeofencesForMapLoadedAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.interactors.app.optics.GeofencesForMapOptic
import com.hypertrack.android.interactors.app.reducer.GeofencesForMapReducer
import com.hypertrack.android.interactors.app.reducer.HistoryReducer
import com.hypertrack.android.interactors.app.reducer.HistoryViewReducer
import com.hypertrack.android.interactors.app.reducer.ScreensReducer
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.NoneScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
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
import com.hypertrack.android.utils.state_machine.effectIf
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.NavGraphDirections

class AppReducer(
    private val useCases: UseCases,
    private val appScope: AppScope,
    private val historyReducer: HistoryReducer,
    private val historyViewReducer: HistoryViewReducer,
    private val screensReducer: ScreensReducer,
    private val geofencesForMapReducer: GeofencesForMapReducer
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
                            viewState = state.splashScreenViewState ?: NoneScreenView
                        )
                        val pushEffect = getHandlePendingPushEffect(
                            action.userState,
                            state.pendingPushNotification
                        )
                        val deeplinkResult = state.pendingDeeplinkResult
                        when (deeplinkResult) {
                            // if there is pending deeplink result, activity is started
                            // we need to login with deeplink or move from SplashScreen
                            is DeeplinkParams -> {
                                initialized
                                    .withEffects(
                                        // still on splash screen
                                        // emits SignedInAction
                                        LoginWithDeeplinkEffect(deeplinkResult) as AppEffect
                                    ).let {
                                        it
                                    }
                                    .withAdditionalEffects(pushEffect)
                            }
                            is DeeplinkError -> {
                                val errorEffect = getDeeplinkErrorEffect(deeplinkResult)
                                initialized.withEffects(
                                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment()) as AppEffect,
                                    errorEffect
                                ).withAdditionalEffects(pushEffect)
                            }
                            NoDeeplink -> {
                                initialized.withEffects(
                                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment()) as AppEffect
                                ).withAdditionalEffects(pushEffect)
                            }
                            null -> {
                                // activity is not started yet
                                // the navigation will be performed on
                                // DeeplinkCheckedAction > Initialized
                                initialized.withEffects(pushEffect)
                            }
                        }
                    }
                    is AppErrorAction -> {
                        handleAction(action, state)
                    }
                    is DeeplinkCheckedAction -> {
                        // if there is DeeplinkCheckedAction, this means that activity is started
                        state.copy(
                            pendingDeeplinkResult = action.deeplinkResult,
                        ).withEffects()
                    }
                    is PushReceivedAction -> {
                        state.copy(pendingPushNotification = action.remoteMessage).withEffects()
                    }
                    is RegisterScreenAction -> {
                        screensReducer.reduce(action, state)
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
                    is DeeplinkLoginErrorAction,
                    is HistoryAppAction,
                    is HistoryViewAppAction,
                    is SignedInAction -> {
                        illegalAction(action, state)
                    }
                    is AppEffectAction -> {
                        state.withEffects(action.appEffect)
                    }
                }
            }
            is AppInitialized -> {
                when (action) {
                    is DeeplinkCheckedAction -> {
                        // activity started
                        when (state.viewState) {
                            SplashScreenView -> {
                                // initial deeplink check (on activity launch)
                                // app is hanging on splash screen and waiting for deeplink check
                                // finished
                                // need to perform initial navigation (to VM or SignIn)
                                when (action.deeplinkResult) {
                                    is DeeplinkParams -> {
                                        // initial navigation will be performed after logging in
                                        // or getting error
                                        state.withEffects(
                                            LoginWithDeeplinkEffect(action.deeplinkResult) as AppEffect
                                        )
                                    }
                                    is DeeplinkError -> {
                                        val errorEffect =
                                            getDeeplinkErrorEffect(action.deeplinkResult)
                                        val newState = state.copy(showProgressbar = false)
                                        when (state.userState) {
                                            is UserLoggedIn -> {
                                                newState.withEffects(
                                                    errorEffect,
                                                    NavigateToUserScopeScreensEffect(state.userState)
                                                )
                                            }
                                            UserNotLoggedIn -> {
                                                newState.withEffects(
                                                    errorEffect,
                                                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment())
                                                )
                                            }
                                        }
                                    }
                                    NoDeeplink -> {
                                        val newState = state.copy(showProgressbar = false)
                                        when (state.userState) {
                                            is UserLoggedIn -> {
                                                newState.withEffects(
                                                    NavigateToUserScopeScreensEffect(state.userState)
                                                )
                                            }
                                            UserNotLoggedIn -> {
                                                newState.withEffects(
                                                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                when (action.deeplinkResult) {
                                    is DeeplinkParams -> {
                                        state.copy(showProgressbar = true).withEffects(
                                            LoginWithDeeplinkEffect(action.deeplinkResult)
                                        )
                                    }
                                    is DeeplinkError -> {
                                        state.copy(showProgressbar = false).withEffects(
                                            ShowAndReportAppErrorEffect(
                                                action.deeplinkResult.exception
                                            )
                                        )
                                    }
                                    NoDeeplink -> {
                                        state.copy(showProgressbar = false).withEffects()
                                    }
                                }
                            }
                        }
                    }
                    is SignedInAction -> {
                        val oldUserState = state.userState
                        val newUserState = action.userState
                        val effects = when (oldUserState) {
                            is UserLoggedIn -> setOf(
                                CleanupUserScopeEffect(
                                    appScope,
                                    oldUserState.userScope
                                )
                            )
                            UserNotLoggedIn -> setOf()
                        } + setOf(
                            NavigateToUserScopeScreensEffect(newUserState)
                        )

                        state.copy(
                            userState = action.userState,
                            showProgressbar = false
                        ).withEffects(effects)
                    }
                    is DeeplinkLoginErrorAction -> {
                        // if the app is waiting on splash screen, we need tto perform
                        // initial navigation
                        val navigationEffect = when (state.viewState) {
                            SplashScreenView -> {
                                when (state.userState) {
                                    is UserLoggedIn -> {
                                        setOf(NavigateToUserScopeScreensEffect(state.userState))
                                    }
                                    UserNotLoggedIn -> {
                                        setOf(NavigateEffect(NavGraphDirections.actionGlobalSignInFragment()))
                                    }
                                }

                            }
                            else -> setOf()
                        }

                        state.copy(showProgressbar = false)
                            .withEffects(
                                ShowAndReportAppErrorEffect(action.exception) as AppEffect
                            )
                            .withAdditionalEffects(navigationEffect)
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
                    is AppEffectAction -> {
                        state.withEffects(action.appEffect)
                    }
                }
            }
        }
    }

    private fun handleAction(action: AppErrorAction, state: AppState)
            : ReducerResult<AppState, AppEffect> {
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
            UserNotLoggedIn -> {
                setOf()
            }
        }
        return state.withEffects(effects)
    }

    private fun illegalAction(
        action: AppAction,
        state: AppState
    ): ReducerResult<AppState, AppEffect> {
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
            UserNotLoggedIn -> {
                setOf()
            }
        }
    }

    private fun getDeeplinkErrorEffect(deeplinkResult: DeeplinkError): AppEffect {
        val exception = deeplinkResult.exception
        val shouldNotShowErrorMessage =
            (exception is BranchErrorException
                    && exception.isBranchConnectionError)
        return if (shouldNotShowErrorMessage) {
            ReportAppErrorEffect(exception)
        } else {
            ShowAndReportAppErrorEffect(exception)
        }
    }

}
