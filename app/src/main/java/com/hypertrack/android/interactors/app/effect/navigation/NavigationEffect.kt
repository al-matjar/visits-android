package com.hypertrack.android.interactors.app.effect.navigation

import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.app.AppInitializedAction
import com.hypertrack.android.interactors.app.DeeplinkCheckedAction
import com.hypertrack.android.interactors.app.DestroyUserScopeEffect
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.action.SignedInAction
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.interactors.app.state.UserState

sealed class NavigationEffect
data class NavigateInGraphEffect(val destination: NavDirections) : NavigationEffect()

// todo use appState in reducer instead of permInteractor to determine navigation destination
class NavigateToUserScopeScreensEffect private constructor(val userState: UserLoggedIn) :
    NavigationEffect() {
    constructor(viewState: SplashScreenView, userState: UserLoggedIn) : this(userState)
    constructor(viewState: SignInScreenView, userState: UserLoggedIn) : this(userState)
    constructor(viewState: SignedInAction, userState: UserLoggedIn) : this(userState)

    override fun toString(): String = javaClass.simpleName
}

class NavigateToSignInEffect private constructor() : NavigationEffect() {
    constructor(viewState: SplashScreenView) : this()
    constructor(effect: DestroyUserScopeEffect) : this()
    constructor(action: AppInitializedAction) : this()
}

fun getNavigateFromSplashScreenEffect(
    viewState: SplashScreenView,
    userState: UserState
): NavigateAppEffect {
    return when (userState) {
        is UserLoggedIn -> NavigateToUserScopeScreensEffect(viewState, userState)
        UserNotLoggedIn -> NavigateToSignInEffect(viewState)
    }.let {
        NavigateAppEffect(it)
    }
}
