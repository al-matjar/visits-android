package com.hypertrack.android.interactors.app.reducer

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
import com.hypertrack.android.interactors.app.NavigateEffect
import com.hypertrack.android.interactors.app.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.ReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.StartTimer
import com.hypertrack.android.interactors.app.StopTimer
import com.hypertrack.android.interactors.app.action.DeeplinkAction
import com.hypertrack.android.interactors.app.action.DeeplinkCheckStartedAction
import com.hypertrack.android.interactors.app.action.DeeplinkCheckTimeoutAction
import com.hypertrack.android.interactors.app.reducer.login.LoginReducer
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.state_machine.effectIf
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.NavGraphDirections

class DeeplinkReducer(
    private val loginReducer: LoginReducer
) {

    fun reduce(
        appAction: AppInitializedAction,
        state: AppNotInitialized,
        initialized: AppInitialized
    ): ReducerResult<out AppState, out AppEffect> {
        val deeplinkResult = state.pendingDeeplinkResult
        return when (deeplinkResult) {
            // if there is pending deeplink result, activity is started
            // we need to login with deeplink or move from SplashScreen
            is DeeplinkParams -> {
                initialized
                    .withEffects(
                        // still on splash screen
                        // emits SignedInAction
                        LoginWithDeeplinkEffect(deeplinkResult) as AppEffect
                    )
            }
            is DeeplinkError -> {
                val errorEffect = getDeeplinkErrorEffect(deeplinkResult)
                initialized.withEffects(
                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment()) as AppEffect,
                    errorEffect
                )
            }
            NoDeeplink -> {
                initialized.withEffects(
                    NavigateEffect(NavGraphDirections.actionGlobalSignInFragment()) as AppEffect
                )
            }
            null -> {
                // activity is not started yet
                // the navigation will be performed on
                // DeeplinkCheckedAction > Initialized
                initialized.withEffects()
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
    ): ReducerResult<out AppState, out AppEffect> {
        // activity started
        return when (state.viewState) {
            SplashScreenView -> {
                // initial deeplink check (on activity launch)
                // app is hanging on splash screen and waiting for deeplink check
                // finished
                // need to perform initial navigation (to VM or SignIn)
                when (action.deeplinkResult) {
                    is DeeplinkParams -> {
                        // initial navigation will be performed after logging in
                        // or getting error
                        if (state.userState is UserLoggedIn) {

                        }
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
        }.withEffects {
            it.effects + StopTimer(DeeplinkCheckTimeoutTimer, state.timerJobs)
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
