package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class UpdateViewStateEffect(val viewState: ViewState) : Effect()
data class SignInEffect(val login: String, val password: String) : Effect() {
    // to avoid showing password in logs
    override fun toString(): String {
        return "${javaClass.simpleName}(login=$login)"
    }
}

data class HandleDeeplinkOrTokenEffect(val text: String, val activity: Activity) : Effect()
data class ErrorEffect(val exception: Exception) : Effect()
object ClearDeeplinkTextEffect : Effect()
