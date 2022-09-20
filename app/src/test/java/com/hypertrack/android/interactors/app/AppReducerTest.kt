package com.hypertrack.android.interactors.app

import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.TestInjector
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.di.AppScopeTest.Companion.appScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.reducer.DeeplinkReducerTest.Companion.deeplinkReducer
import com.hypertrack.android.interactors.app.reducer.HistoryReducerTest.Companion.historyReducer
import com.hypertrack.android.interactors.app.reducer.ScreensReducer
import com.hypertrack.android.interactors.app.reducer.login.LoginReducerTest.Companion.loginReducer
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import io.mockk.every
import io.mockk.mockk

class AppReducerTest {

    companion object {
        fun appReducer(): AppReducer {
            return AppReducer(
                appScope = appScope(),
                useCases = mockk(),
                historyReducer = historyReducer(),
                screensReducer = mockk(),
                historyViewReducer = mockk(),
                geofencesForMapReducer = mockk(),
                deeplinkReducer = deeplinkReducer(),
                timerReducer = mockk(),
                loginReducer = loginReducer(),
            )
        }

        fun appNotInitialized(
            pendingDeeplinkResult: DeeplinkResult? = null,
            pendingPushNotification: RemoteMessage? = null
        ): AppNotInitialized {
            return AppNotInitialized(
                appScope = mockk(),
                useCases = mockk(),
                pendingDeeplinkResult = pendingDeeplinkResult,
                pendingPushNotification = pendingPushNotification,
                splashScreenViewState = null
            )
        }

        fun appInitialized(
            tripsInteractorParam: TripsInteractor = mockk(),
            userState: UserState? = null,
            viewState: AppViewState = SplashScreenView,
            showProgressbar: Boolean = false,
            userIsLoggingIn: UserData? = null
        ): AppInitialized {
            return AppInitialized(
                appScope = mockk(),
                useCases = mockk(),
                tripCreationScope = null,
                userState = userState ?: UserLoggedIn(
                    deviceId = TestInjector.TEST_DEVICE_ID,
                    trackingState = TrackingStateUnknown,
                    userScope = mockk {
                        every { tripsInteractor } returns tripsInteractorParam
                    },
                    userData = mockk(),
                    history = mockk(),
                    userLocation = mockk(),
                    useCases = mockk()
                ),
                viewState = viewState,
                showProgressbar = showProgressbar,
                timerJobs = mockk(),
                userIsLoggingIn = userIsLoggingIn
            )
        }

        fun userLoggedIn(
            userScope: UserScope = mockk(),
            deviceId: DeviceId = TestInjector.TEST_DEVICE_ID,
            userData: UserData? = null
        ): UserLoggedIn {
            return UserLoggedIn(
                deviceId = deviceId,
                trackingState = TrackingStateUnknown,
                userScope = userScope,
                userData = userData ?: mockk(),
                history = mockk(),
                userLocation = mockk(),
                useCases = mockk()
            )
        }

        fun validDeeplinkParams(
            email: String = TestInjector.TEST_EMAIL.value,
            publishableKey: String = TestInjector.TEST_PUBLISHABLE_KEY.value,
            url: String = TestInjector.TEST_URL
        ): DeeplinkParams {
            return DeeplinkParams(
                mapOf(
                    "publishable_key" to publishableKey,
                    "email" to email,
                    "~referring_link" to url
                )
            )
        }
    }
}
