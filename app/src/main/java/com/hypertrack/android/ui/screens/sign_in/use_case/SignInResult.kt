package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.use_case.login.CognitoLoginError
import com.hypertrack.android.use_case.login.LoggedIn

sealed class SignInResult
object SignInInvalidLoginOrPassword : SignInResult()
object SignInNoSuchUser : SignInResult()
object ConfirmationRequired : SignInResult()
data class SignInSuccess(val loggedIn: LoggedIn) : SignInResult()
