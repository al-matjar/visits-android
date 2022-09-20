package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.interactors.app.action.InitiateLoginAction

sealed class SignInResult
object SignInInvalidLoginOrPassword : SignInResult()
object SignInNoSuchUser : SignInResult()
object ConfirmationRequired : SignInResult()
data class SignInSuccess(val loginAction: InitiateLoginAction) : SignInResult()
