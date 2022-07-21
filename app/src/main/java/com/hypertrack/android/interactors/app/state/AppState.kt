package com.hypertrack.android.interactors.app.state

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.use_case.sdk.TrackingStarted

sealed class AppState {
    fun isSdkTracking(): Boolean {
        return when (this) {
            is AppNotInitialized -> false
            is AppInitialized -> when (userState) {
                UserNotLoggedIn -> false
                is UserLoggedIn -> userState.trackingState is TrackingStarted
            }
        }
    }

    fun isProgressbarVisible(): Boolean {
        return when (this) {
            is AppInitialized -> this.showProgressbar
            is AppNotInitialized -> false
        }
    }

}

data class AppNotInitialized(
    val appScope: AppScope,
    val useCases: UseCases,
    // non-null if splash screen is opened before app init
    val splashScreenViewState: SplashScreenView?,
    // if deeplink result is received before the init, it is saved here
    val pendingDeeplinkResult: DeeplinkResult?,
    // if push notification is received before the init, it is saved here
    val pendingPushNotification: RemoteMessage?
) : AppState()

data class AppInitialized(
    val appScope: AppScope,
    val useCases: UseCases,
    val userState: UserState,
    val tripCreationScope: TripCreationScope?,
    val viewState: AppViewState,
    // todo move to view state
    val showProgressbar: Boolean = false,
) : AppState()
