package com.hypertrack.android.interactors.app

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.IllegalActionException
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ReducerResult
import java.lang.IllegalStateException

class AppReducer(
    private val useCases: UseCases,
    private val appScope: AppScope,
) {

    fun reduce(state: AppState, action: AppAction): ReducerResult<AppState, Effect> {
        return when (state) {
            is NotInitialized -> {
                when (action) {
                    is InitAppAction -> {
                        state.withEffects(InitAppEffect(state.appScope))
                    }
                    is AppInitializedAction -> {
                        // app is initialized
                        val initialized = Initialized(
                            state.appScope,
                            useCases,
                            action.userState,
                            tripCreationScope = null,
                            viewState = state.viewState
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
                                        LoginWithDeeplinkEffect(deeplinkResult)
                                    )
                                    .withEffects(pushEffect)
                            }
                            is DeeplinkError -> {
                                initialized.copy(viewState = SignInState)
                                    .withEffects(
                                        NavigateToSignInEffect,
                                        ShowAppErrorMessageEffect(deeplinkResult.exception)
                                    )
                                    .withEffects(pushEffect)
                            }
                            NoDeeplink -> {
                                initialized.copy(viewState = SignInState)
                                    .withEffects(
                                        NavigateToSignInEffect
                                    )
                                    .withEffects(pushEffect)
                            }
                            null -> {
                                // activity is not started yet
                                // the navigation will be performed on
                                // DeeplinkCheckedAction > Initialized
                                initialized
                                    .withEffects(pushEffect)
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
                            viewState = SplashScreenState
                        ).withEffects()
                    }
                    is PushReceivedAction -> {
                        state.copy(pendingPushNotification = action.remoteMessage).withEffects()
                    }
                    is TrackingStateChangedAction -> {
                        state.withEffects()
                    }
                    SplashScreenOpenedAction -> {
                        state.withEffects()
                    }
                    is CreateTripCreationScopeAction,
                    DestroyTripCreationScopeAction,
                    is DeeplinkLoginErrorAction,
                    is SignedInAction -> {
                        illegalAction(action, state)
                    }
                }
            }
            is Initialized -> {
                when (action) {
                    is DeeplinkCheckedAction -> {
                        // activity started
                        when (state.viewState) {
                            SplashScreenState -> {
                                // initial deeplink check (on activity launch)
                                // app is hanging on splash screen and waiting for deeplink check
                                // finished
                                // need to perform initial navigation (to VM or SignIn)
                                when (action.deeplinkResult) {
                                    is DeeplinkParams -> {
                                        // initial navigation will be performed after logging in
                                        // or getting error
                                        state.withEffects(
                                            LoginWithDeeplinkEffect(action.deeplinkResult)
                                        )
                                    }
                                    is DeeplinkError -> {
                                        when (state.userState) {
                                            is UserLoggedIn -> {
                                                state.copy(viewState = UserScopeScreensState)
                                                    .withEffects(
                                                        ShowAppErrorMessageEffect(
                                                            action.deeplinkResult.exception
                                                        ),
                                                        NavigateToUserScopeScreensEffect(state.userState)
                                                    )
                                            }
                                            UserNotLoggedIn -> {
                                                state.copy(viewState = SignInState).withEffects(
                                                    ShowAppErrorMessageEffect(
                                                        action.deeplinkResult.exception
                                                    ),
                                                    NavigateToSignInEffect
                                                )
                                            }
                                        }
                                    }
                                    NoDeeplink -> {
                                        when (state.userState) {
                                            is UserLoggedIn -> {
                                                state.copy(viewState = UserScopeScreensState)
                                                    .withEffects(
                                                        NavigateToUserScopeScreensEffect(state.userState)
                                                    )
                                            }
                                            UserNotLoggedIn -> {
                                                state.copy(viewState = SignInState).withEffects(
                                                    NavigateToSignInEffect
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            SignInState, UserScopeScreensState -> {
                                when (action.deeplinkResult) {
                                    is DeeplinkParams -> {
                                        state.copy(showProgressbar = true).withEffects(
                                            LoginWithDeeplinkEffect(action.deeplinkResult)
                                        )
                                    }
                                    is DeeplinkError -> {
                                        state.withEffects(
                                            ShowAppErrorMessageEffect(
                                                action.deeplinkResult.exception
                                            )
                                        )
                                    }
                                    NoDeeplink -> {
                                        state.withEffects()
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
                                ),
                                SetCrashReportingDeviceIdentifier(
                                    appScope,
                                    oldUserState.deviceId
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
                            SplashScreenState -> {
                                when (state.userState) {
                                    is UserLoggedIn -> {
                                        setOf(NavigateToUserScopeScreensEffect(state.userState))
                                    }
                                    UserNotLoggedIn -> {
                                        setOf(NavigateToSignInEffect)
                                    }
                                }

                            }
                            SignInState, UserScopeScreensState -> setOf()
                        }

                        state.copy(showProgressbar = false)
                            .withEffects(
                                ShowAppErrorMessageEffect(action.exception)
                            )
                            .withEffects(navigationEffect)
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
                                ).withEffects()
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
                    SplashScreenOpenedAction -> {
                        // activity was killed and now started again
                        // need to reset view state
                        state.copy(viewState = SplashScreenState).withEffects()
                    }
                }
            }
        }
    }

    private fun handleAction(action: AppErrorAction, state: AppState)
            : ReducerResult<AppState, Effect> {
        return state.withEffects(
            ShowAppErrorMessageEffect(action.exception)
        )
    }

    private fun handleAction(
        action: PushReceivedAction,
        state: AppState,
        appScope: AppScope,
        userState: UserState
    ): ReducerResult<AppState, Effect> {
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

    private fun illegalAction(action: AppAction, state: AppState): ReducerResult<AppState, Effect> {
        return IllegalActionException(action, state).let {
            if (MyApplication.DEBUG_MODE) {
                throw it
            } else {
                state.withEffects(
                    ShowAppErrorMessageEffect(it)
                )
            }
        }
    }

    private fun getHandlePendingPushEffect(
        userState: UserState,
        pendingPushNotification: RemoteMessage?
    ): Set<Effect> {
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

    private fun getSignInEffects(
        oldUserState: UserState,
        appScope: AppScope,
        newUserState: UserLoggedIn
    ): Set<Effect> {
        return when (oldUserState) {
            is UserLoggedIn -> setOf(
                CleanupUserScopeEffect(
                    appScope,
                    oldUserState.userScope
                ),
                SetCrashReportingDeviceIdentifier(
                    appScope,
                    oldUserState.deviceId
                )
            )
            UserNotLoggedIn -> setOf()
        } + setOf(
            NavigateToUserScopeScreensEffect(
                newUserState
            )
        )
    }

}
