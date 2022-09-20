package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.utils.HardwareId

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class UpdateViewStateEffect(val viewState: ViewState) : Effect()
data class SignInEffect(val login: String, val password: String) : Effect() {
    // to avoid showing password in logs
    override fun toString(): String {
        // todo enable logging emails everywhere after moving to Sentry
        // as it is hosted on Hypertrack servers
        return javaClass.simpleName
    }
}

data class HandleDeeplinkOrTokenEffect(val text: String, val activity: Activity) : Effect()
data class ErrorEffect(val exception: Exception) : Effect()
object PrepareOnDeeplinkIssuesClickedActionEffect : Effect()
object ClearDeeplinkTextEffect : Effect()
data class CopyHardwareIdEffect(val hardwareId: HardwareId) : Effect()
data class AppActionEffect(val action: AppAction) : Effect()
