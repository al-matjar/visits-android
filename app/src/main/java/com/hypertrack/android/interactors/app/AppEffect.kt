package com.hypertrack.android.interactors.app

import android.content.Context
import androidx.navigation.NavDirections
import com.fonfon.kgeohash.GeoHash
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.effect.MapEffect
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.Effect
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.utils.message.AppMessage
import kotlinx.coroutines.Job
import java.lang.Exception
import java.time.LocalDate

sealed class AppEffect {
    override fun toString(): String = javaClass.simpleName
}

data class InitAppEffect(
    val appScope: AppScope
) : AppEffect()

data class LoginWithDeeplinkEffect(
    val deeplinkParams: DeeplinkParams,
) : AppEffect()

data class LoginWithPublishableKey(
    val publishableKey: RealPublishableKey,
    val userData: UserData,
    val oldUserState: UserLoggedIn?,
) : AppEffect()

data class HandlePushEffect(
    val userState: UserLoggedIn,
    val context: Context,
    val remoteMessage: RemoteMessage
) : AppEffect()

data class NotifyAppStateUpdateEffect(
    val newState: AppState
) : AppEffect() {
    override fun toString(): String {
        return javaClass.simpleName
    }
}

data class ShowAndReportAppErrorEffect(
    val exception: Exception
) : AppEffect()

data class OnlyShowAppErrorEffect(
    val exception: Exception
) : AppEffect()

data class ShowAppMessageEffect(
    val message: AppMessage
) : AppEffect()

data class ReportAppErrorEffect(
    val exception: Exception
) : AppEffect()

// todo use appState instead of permInteractor to determine navigation destination
data class NavigateToUserScopeScreensEffect(
    val userState: UserLoggedIn
) : AppEffect() {
    override fun toString(): String = javaClass.simpleName
}

data class NavigateEffect(val destination: NavDirections) : AppEffect()

data class AppEventEffect(val event: AppEvent) : AppEffect()

data class LoadHistoryEffect(val date: LocalDate, val userScope: UserScope) : AppEffect()

data class AppMapEffect(val mapEffect: MapEffect) : AppEffect()

data class HistoryViewEffect(val effect: Effect) : AppEffect()

data class AppActionEffect(val action: AppAction) : AppEffect()

data class DestroyUserScopeEffect(val userScope: UserScope) : AppEffect()

data class LoadGeofencesForMapEffect(
    val geoHash: GeoHash,
    val pageToken: String?,
    val useCases: UserScopeUseCases
) : AppEffect()

data class StartTimer(val timer: Timer) : AppEffect()
data class StopTimer(val timer: Timer, val timerJobs: Map<Timer, Job>) : AppEffect() {
    override fun toString(): String = javaClass.simpleName
}


