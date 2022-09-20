package com.hypertrack.android.interactors.app

import com.hypertrack.android.TestInjector
import com.hypertrack.android.assertEffects
import com.hypertrack.android.assertNoEffects
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appNotInitialized
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUseCaseTest.Companion.deeplinkParams
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

@Suppress("ComplexRedundantLet")
class AppInitializedActionTest {

    @Test
    fun `Created - AppInitializedAction(not logged in)`() {
        val state = appNotInitialized()
        val action = appInitializedAction(UserNotLoggedIn)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertNoEffects()
            }
        }
    }

    @Test
    fun `Created (pending deeplink) - AppInitializedAction(not logged in)`() {
        val state = appNotInitialized(pendingDeeplinkResult = deeplinkParams())
        val action = appInitializedAction(UserNotLoggedIn)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertEquals(1, result.effects.size)
                assertTrue(result.effects.first() is LoginWithDeeplinkEffect)
            }
        }
    }

    @Test
    fun `Created - AppInitializedAction(logged in)`() {
        val state = appNotInitialized(pendingPushNotification = mockk())
        // default user state is UserLoggedIn
        val action = appInitializedAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertEffects(
                    HandlePushEffect::class
                )
            }
        }
    }

    @Test
    fun `Created (pending deeplink) - AppInitializedAction(logged in)`() {
        val state = appNotInitialized(
            pendingPushNotification = mockk(),
            pendingDeeplinkResult = deeplinkParams()
        )
        // default user state is UserLoggedIn
        val action = appInitializedAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertEffects(
                    HandlePushEffect::class,
                    LoginWithDeeplinkEffect::class
                )
            }
        }
    }

    companion object {
        fun appInitializedAction(
            userState: UserState? = null
        ): AppInitializedAction {
            return AppInitializedAction(
                userState = userState ?: UserLoggedIn(
                    deviceId = TestInjector.TEST_DEVICE_ID,
                    trackingState = TrackingStateUnknown,
                    userScope = mockk(),
                    userData = mockk(),
                    history = mockk(),
                    userLocation = mockk(),
                    useCases = mockk()
                )
            )
        }
    }

}
