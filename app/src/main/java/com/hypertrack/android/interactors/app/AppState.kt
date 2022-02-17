package com.hypertrack.android.interactors.app

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.use_case.sdk.TrackingStarted
import com.hypertrack.android.utils.ReducerResult

sealed class AppState {
    fun isSdkTracking(): Boolean {
        return when (this) {
            is NotInitialized -> false
            is Initialized -> when (userState) {
                UserNotLoggedIn -> false
                is UserLoggedIn -> userState.trackingState is TrackingStarted
            }
        }
    }

    fun isProgressbarVisible(): Boolean {
        return when (this) {
            is Initialized -> this.showProgressbar
            is NotInitialized -> false
        }
    }
}

data class NotInitialized(
    val appScope: AppScope,
    val useCases: UseCases,
    val viewState: AppViewState,
    // if deeplink result is received before the init, it is saved here
    val pendingDeeplinkResult: DeeplinkResult?,
    // if push notification is received before the init, it is saved here
    val pendingPushNotification: RemoteMessage?
) : AppState()

data class Initialized(
    val appScope: AppScope,
    val useCases: UseCases,
    val userState: UserState,
    val tripCreationScope: TripCreationScope?,
    val viewState: AppViewState,
    // todo move to view state
    val showProgressbar: Boolean = false,
) : AppState()

fun AppState.withEffects(effects: Set<Effect>): ReducerResult<AppState, Effect> {
    return ReducerResult(
        this,
        effects
    )
}

fun AppState.withEffects(vararg effect: Effect): ReducerResult<AppState, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}
