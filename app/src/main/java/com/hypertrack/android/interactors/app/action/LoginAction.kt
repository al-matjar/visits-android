package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.user.UserData

sealed class LoginAction
data class InitiateLoginAction(
    val publishableKey: RealPublishableKey,
    val userData: UserData
) : LoginAction()

data class LoginErrorAction(
    val exception: Exception
) : LoginAction()

data class SignedInAction(
    val userState: UserLoggedIn
) : LoginAction()
