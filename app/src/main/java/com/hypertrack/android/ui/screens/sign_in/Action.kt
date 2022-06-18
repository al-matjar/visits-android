package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

data class DeeplinkOrTokenPastedAction(val text: String, val activity: Activity) : Action()
data class LoginChangedAction(val login: String) : Action()
object OnLoginClickAction : Action()
object OnDeeplinkIssuesClickAction : Action()
object OnCloseClickAction : Action()
data class PasswordChangedAction(val password: String) : Action() {
    // to not show passwords in logs
    override fun toString(): String = javaClass.simpleName
}

data class ErrorAction(val exception: Exception) : Action()
