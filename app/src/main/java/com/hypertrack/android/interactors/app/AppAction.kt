package com.hypertrack.android.interactors.app

import android.app.Application
import android.content.Intent
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.interactors.app.action.HistoryAction
import com.hypertrack.android.interactors.app.state.Screen
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewAction
import com.hypertrack.android.use_case.handle_push.formatToString
import com.hypertrack.android.use_case.sdk.NewTrackingState

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

data class TrackingStateChangedAction(val trackingState: NewTrackingState) : AppAction()
data class UserLocationChangedAction(val userLocation: LatLng?) : AppAction()
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
data class ActivityOnNewIntent(val intent: Intent?) : AppAction()
data class HistoryAppAction(val historyAction: HistoryAction) : AppAction()
data class HistoryViewAppAction(val historyViewAction: HistoryViewAction) : AppAction()
data class RegisterScreenAction(val screen: Screen) : AppAction()
