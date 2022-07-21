package com.hypertrack.android.interactors.app

import com.hypertrack.android.TestInjector
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.createdState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUseCaseTest.Companion.deeplinkParams
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

@Suppress("ComplexRedundantLet")
class AppInitializedActionTest {

    @Test
    fun `Created - AppInitializedAction`() {
        val state = createdState()
        val action = appInitializedAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertEquals(0, result.effects.size)
            }
        }
    }

    @Test
    fun `Created (pending deeplink) - AppInitializedAction`() {
        val state = createdState(pendingDeeplinkResult = deeplinkParams())
        val action = appInitializedAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertEquals(1, result.effects.size)
                assertTrue(result.effects.first() is LoginWithDeeplinkEffect)
            }
        }
    }

    companion object {
        fun appInitializedAction(): AppInitializedAction {
            return AppInitializedAction(
                userState = UserLoggedIn(
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
