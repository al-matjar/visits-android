package com.hypertrack.android.interactors.app

import android.content.Context
import androidx.navigation.NavDirections
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.local.DeviceId
import java.lang.Exception

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class InitAppEffect(
    val appScope: AppScope
) : Effect()

data class LoginWithDeeplinkEffect(
    val deeplinkParams: DeeplinkParams,
) : Effect()

data class CleanupUserScopeEffect(
    val appScope: AppScope,
    val oldUserScope: UserScope
) : Effect()

data class HandlePushEffect(
    val userState: UserLoggedIn,
    val context: Context,
    val remoteMessage: RemoteMessage
) : Effect()

data class SetCrashReportingDeviceIdentifier(
    val appScope: AppScope,
    val deviceId: DeviceId
) : Effect()

data class NotifyAppStateUpdateEffect(
    val newState: AppState
) : Effect()

data class ShowAppErrorMessageEffect(
    val exception: Exception
) : Effect()

// todo use appState instead of permInteractor to determine navigation destination
data class NavigateToUserScopeScreensEffect(
    val newUserState: UserLoggedIn
) : Effect()

// todo create generic navigation effect
object NavigateToSignInEffect : Effect()
