package com.hypertrack.android.interactors.app.reducer.login

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.LoginWithPublishableKey
import com.hypertrack.android.interactors.app.NavigateEffect
import com.hypertrack.android.interactors.app.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.ReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.ShowAppMessageEffect
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.interactors.app.action.LoginAction
import com.hypertrack.android.interactors.app.action.LoginErrorAction
import com.hypertrack.android.interactors.app.action.SignedInAction
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.utils.asSet
import com.hypertrack.android.utils.message.LoggedInMessage
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.state_machine.effectIf
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.NavGraphDirections

class LoginReducer(private val appScope: AppScope) {

    fun reduce(
        action: LoginAction,
        state: AppInitialized
    ): ReducerResult<out AppInitialized, out AppEffect> {
        return when (action) {
            is InitiateLoginAction -> {
                if (state.userState is UserLoggedIn && state.userState.userData == action.userData) {
                    // skipping login attempt for the same user
                    val navigationEffect = effectIf(state.viewState is SplashScreenView) {
                        NavigateToUserScopeScreensEffect(state.userState)
                    }
                    val errorEffect = ShowAndReportAppErrorEffect(AlreadyLoggedInException())
                    state.copy(showProgressbar = false).withEffects(
                        navigationEffect + errorEffect
                    )
                } else {
                    if (state.userIsLoggingIn == null) {
                        state.copy(
                            showProgressbar = true,
                            userIsLoggingIn = action.userData
                        ).withEffects(
                            LoginWithPublishableKey(
                                action.publishableKey,
                                action.userData,
                                oldUserState = if (state.userState is UserLoggedIn) {
                                    state.userState
                                } else null
                            )
                        )
                    } else {
                        state.copy(showProgressbar = false).withEffects(
                            ShowAndReportAppErrorEffect(LoginAlreadyInProgressException())
                        )
                    }
                }
            }
            is SignedInAction -> {
                val newUserState = action.userState
                val navigationEffect = NavigateToUserScopeScreensEffect(newUserState).asSet()
                val loginMessageEffect = ShowAppMessageEffect(
                    LoggedInMessage(action.userState.userData)
                )
                return state.copy(
                    userState = action.userState,
                    showProgressbar = false,
                    userIsLoggingIn = null
                ).withEffects(navigationEffect + loginMessageEffect)
            }
            is LoginErrorAction -> {
                // if the app is waiting on splash screen, we need to perform
                // initial navigation
                val navigationEffect = when (state.viewState) {
                    SplashScreenView -> {
                        // the only type of login that can lead to this code path is deeplink login
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
                val errorEffect = ShowAndReportAppErrorEffect(action.exception)

                return state.copy(
                    showProgressbar = false,
                    userIsLoggingIn = null
                ).withEffects(
                    navigationEffect + errorEffect
                )
            }
        }
    }

}
