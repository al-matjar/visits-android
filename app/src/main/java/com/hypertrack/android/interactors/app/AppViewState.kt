package com.hypertrack.android.interactors.app

sealed class AppViewState {
    override fun toString(): String = javaClass.simpleName
}

object SplashScreenState : AppViewState()
object SignInState : AppViewState()
object UserScopeScreensState : AppViewState()
