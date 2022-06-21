package com.hypertrack.android.interactors.app

import com.hypertrack.android.TestInjector
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.reducer.ScreensReducer
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import io.mockk.every
import io.mockk.mockk

class AppReducerTest {

    companion object {
        fun appReducer(): AppReducer {
            return AppReducer(
                appScope = mockk(),
                useCases = mockk(),
                historyReducer = mockk(),
                screensReducer = mockk(),
                historyViewReducer = mockk()
            )
        }

        fun createdState(pendingDeeplinkResult: DeeplinkResult? = null): AppNotInitialized {
            return AppNotInitialized(
                appScope = mockk(),
                useCases = mockk(),
                pendingDeeplinkResult = pendingDeeplinkResult,
                pendingPushNotification = null,
                splashScreenViewState = null
            )
        }

        fun initializedState(
            tripsInteractorParam: TripsInteractor = mockk(),
            userState: UserState? = null,
            viewState: AppViewState = SplashScreenView,
            showProgressbar: Boolean = false
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
                    userLocation = mockk()
                ),
                viewState = viewState,
                showProgressbar = showProgressbar
            )
        }

        fun userLoggedIn(
            userScope: UserScope = mockk(),
            deviceId: DeviceId = TestInjector.TEST_DEVICE_ID
        ): UserLoggedIn {
            return UserLoggedIn(
                deviceId = deviceId,
                trackingState = TrackingStateUnknown,
                userScope = userScope,
                userData = mockk(),
                history = mockk(),
                userLocation = mockk()
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
