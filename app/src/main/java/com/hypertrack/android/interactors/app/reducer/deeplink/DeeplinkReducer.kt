package com.hypertrack.android.interactors.app.reducer.deeplink

import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppInitializedAction
import com.hypertrack.android.interactors.app.DeeplinkCheckTimeoutTimer
import com.hypertrack.android.interactors.app.DeeplinkCheckedAction
import com.hypertrack.android.interactors.app.LoginWithDeeplinkEffect
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.ReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.StartTimer
import com.hypertrack.android.interactors.app.StopTimer
import com.hypertrack.android.interactors.app.action.DeeplinkAction
import com.hypertrack.android.interactors.app.action.DeeplinkCheckStartedAction
import com.hypertrack.android.interactors.app.action.DeeplinkCheckTimeoutAction
import com.hypertrack.android.interactors.app.action.TimerAction
import com.hypertrack.android.interactors.app.action.TimerEndedAction
import com.hypertrack.android.interactors.app.action.TimerStartedAction
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToSignInEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.reducer.login.LoginReducer
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.NoneScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.state_machine.effectIf
import com.hypertrack.android.utils.withEffects

class DeeplinkReducer(
    private val loginReducer: LoginReducer
) {

    fun reduce(
        appAction: AppInitializedAction,
        oldState: AppNotInitialized,
        newState: AppInitialized
    ): ReducerResult<out AppState, out AppEffect> {
        val deeplinkResult = oldState.pendingDeeplinkResult
        return when (deeplinkResult) {
            // if there is pending deeplink result, activity is started
            // we need to login with deeplink or move from SplashScreen
            is DeeplinkParams -> {
                newState
                    .withEffects(
                        // still on splash screen
                        // emits SignedInAction
                        LoginWithDeeplinkEffect(deeplinkResult) as AppEffect
                    )
            }
            is DeeplinkError -> {
                val errorEffect = getDeeplinkErrorEffect(deeplinkResult)
                newState.withEffects(
                    NavigateAppEffect(NavigateToSignInEffect(appAction)) as AppEffect,
                    errorEffect
                )
            }
            NoDeeplink -> {
                newState.withEffects(
                    NavigateAppEffect(NavigateToSignInEffect(appAction)) as AppEffect
                )
            }
            null -> {
                // activity is not started yet
                // the navigation will be performed on
                // DeeplinkCheckedAction > Initialized
                newState.withEffects()
            }
        }
    }

    fun reduce(
        action: DeeplinkCheckedAction,
        state: AppNotInitialized
    ): ReducerResult<out AppState, out AppEffect> {
        // if there is DeeplinkCheckedAction, this means that activity is started
        return state.copy(
            pendingDeeplinkResult = action.deeplinkResult,
        ).withEffects(
            StopTimer(DeeplinkCheckTimeoutTimer, state.timerJobs)
        )
    }

    fun reduce(
        action: DeeplinkCheckedAction,
        state: AppInitialized
    ): ReducerResult<AppInitialized, out AppEffect> {
        // activity started
        val timerEffect = StopTimer(DeeplinkCheckTimeoutTimer, state.timerJobs)
        return when (val viewState = state.viewState) {
            is SplashScreenView -> {
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
                                val navigateEffect = NavigateAppEffect(
                                    NavigateToUserScopeScreensEffect(viewState, state.userState)
                                )
                                newState.withEffects(
                                    errorEffect,
                                    navigateEffect
                                )
                            }
                            UserNotLoggedIn -> {
                                val navigateEffect =
                                    NavigateAppEffect(NavigateToSignInEffect(viewState))
                                newState.withEffects(
                                    errorEffect,
                                    navigateEffect
                                )
                            }
                        }
                    }
                    NoDeeplink -> {
                        val newState = state.copy(showProgressbar = false)
                        when (state.userState) {
                            is UserLoggedIn -> {
                                newState.withEffects(
                                    NavigateAppEffect(
                                        NavigateToUserScopeScreensEffect(viewState, state.userState)
                                    )
                                )
                            }
                            UserNotLoggedIn -> {
                                newState.withEffects(
                                    NavigateAppEffect(NavigateToSignInEffect(viewState))
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
                            getDeeplinkErrorEffect(action.deeplinkResult)
                        )
                    }
                    NoDeeplink -> {
                        state.copy(showProgressbar = false).withEffects()
                    }
                }
            }
        }.withEffects {
            it.effects + timerEffect
        }
    }

    fun reduce(
        action: DeeplinkAction,
        state: AppState
    ): ReducerResult<out AppState, out AppEffect> {
        return when (state) {
            is AppNotInitialized -> {
                when (action) {
                    DeeplinkCheckStartedAction -> {
                        state.withEffects(
                            StartTimer(DeeplinkCheckTimeoutTimer)
                        )
                    }
                    DeeplinkCheckTimeoutAction -> {
                        val newState = if (state.pendingDeeplinkResult == null) {
                            state.copy(pendingDeeplinkResult = NoDeeplink)
                        } else {
                            state
                        }
                        newState.withEffects()
                    }
                }
            }
            is AppInitialized -> {
                when (action) {
                    DeeplinkCheckStartedAction -> {
                        state.withEffects(effectIf(state.viewState is SplashScreenView) {
                            StartTimer(DeeplinkCheckTimeoutTimer)
                        })
                    }
                    DeeplinkCheckTimeoutAction -> {
                        state.withEffects(
                            AppActionEffect(DeeplinkCheckedAction(NoDeeplink))
                        )
                    }
                }
            }
        }
    }

    fun reduce(
        action: TimerEndedAction,
        timer: DeeplinkCheckTimeoutTimer,
        state: AppState
    ): ReducerResult<out AppState, out AppEffect> {
        // we are unable to stop deeplink check event from firing after timeout
        // it will be handled in the same way as the case when user clicks a deeplink
        return when (state) {
            is AppInitialized -> {
                reduce(DeeplinkCheckedAction(NoDeeplink), state).withEffects {
                    it.effects + ShowAndReportAppErrorEffect(DeeplinkTimeoutException())
                }
            }
            is AppNotInitialized -> {
                // state with low probability (app init lasts more than DEEPLINK_CHECK_TIMEOUT)
                reduce(DeeplinkCheckedAction(NoDeeplink), state)
            }
        }.withEffects {
            it.effects + ShowAndReportAppErrorEffect(DeeplinkTimeoutException())
        }
    }

    private fun getDeeplinkErrorEffect(deeplinkResult: DeeplinkError): AppEffect {
        val exception = deeplinkResult.exception
        // we don't show Branch errors because in this case we try getting data from HT backend
        val shouldNotShowErrorMessage =
            (exception is BranchErrorException
                    && exception.isBranchConnectionError)
        return if (shouldNotShowErrorMessage) {
            ReportAppErrorEffect(exception)
        } else {
            ShowAndReportAppErrorEffect(exception)
        }
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

    companion object {
        val DEEPLINK_CHECK_TIMEOUT = 5000L
    }

}
