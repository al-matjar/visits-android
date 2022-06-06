package com.hypertrack.android.interactors.app

import android.app.Application
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.use_case.handle_push.formatToString
import com.hypertrack.android.use_case.sdk.NewTrackingState
import com.hypertrack.android.utils.TrackingStateValue

sealed class AppAction {
    override fun toString(): String = javaClass.simpleName
}

data class InitAppAction(
    val application: Application
) : AppAction()

data class AppInitializedAction(
    val userState: UserState,
) : AppAction()

data class DeeplinkCheckedAction(val deeplinkResult: DeeplinkResult) : AppAction()
data class SignedInAction(val userState: UserLoggedIn) : AppAction()
data class DeeplinkLoginErrorAction(val exception: Exception) : AppAction()

object SplashScreenOpenedAction : AppAction()

data class TrackingStateChangedAction(val trackingState: NewTrackingState) : AppAction()
data class PushReceivedAction(
    val remoteMessage: RemoteMessage
) : AppAction() {
    override fun toString(): String {
        return "PushReceivedAction(remoteMessage=${remoteMessage.formatToString()})"
    }
}

data class CreateTripCreationScopeAction(val destinationData: DestinationData) : AppAction()
object DestroyTripCreationScopeAction : AppAction()
data class AppErrorAction(val exception: Exception) : AppAction()

//class UserTokenRetrievedAction(
//    val token: UserToken,
//    val authData: UserAuthData,
//    val deviceId: DeviceId
//) : Action()
