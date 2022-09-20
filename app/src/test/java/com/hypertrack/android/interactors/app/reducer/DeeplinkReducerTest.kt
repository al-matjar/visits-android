package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appNotInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.interactors.app.DeeplinkCheckTimeoutTimer
import com.hypertrack.android.interactors.app.DeeplinkCheckedAction
import com.hypertrack.android.interactors.app.StartTimer
import com.hypertrack.android.interactors.app.action.DeeplinkCheckStartedAction
import com.hypertrack.android.interactors.app.action.DeeplinkCheckTimeoutAction
import com.hypertrack.android.interactors.app.reducer.login.LoginReducerTest.Companion.loginReducer
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.lang.Exception

class DeeplinkReducerTest {

    @Test
    fun `DeeplinkCheckStarted - AppNotInitialized`() {
        appNotInitialized().let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckStartedAction, oldState).let {
                assertEquals(oldState, it.newState)
                assertTrue(it.effects.size == 1)
                assertEquals(
                    DeeplinkCheckTimeoutTimer,
                    it.effects.filterIsInstance<StartTimer>().first().timer
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckStarted - AppInitialized(splash screen)`() {
        appInitialized(viewState = SplashScreenView).let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckStartedAction, oldState).let {
                assertEquals(oldState, it.newState)
                assertEquals(1, it.effects.size)
                assertEquals(
                    DeeplinkCheckTimeoutTimer,
                    it.effects.filterIsInstance<StartTimer>().first().timer
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckStarted - AppInitialized(not splash screen)`() {
        appInitialized(viewState = notSplashScreenViewState()).let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckStartedAction, oldState).let {
                assertEquals(oldState, it.newState)
                assertEquals(0, it.effects.size)
            }
        }
    }

    @Test
    fun `DeeplinkCheckTimeout - AppNotInitialized(no pendingDeeplinkResult)`() {
        appNotInitialized(pendingDeeplinkResult = null).let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckTimeoutAction, oldState).let {
                (it.newState as AppNotInitialized).let { newState ->
                    assertEquals(NoDeeplink, newState.pendingDeeplinkResult)
                    assertEquals(0, it.effects.size)
                }
            }
        }
    }

    @Test
    fun `DeeplinkCheckTimeout - AppNotInitialized(pendingDeeplinkResult)`() {
        val result = validDeeplinkParams()
        appNotInitialized(pendingDeeplinkResult = result).let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckTimeoutAction, oldState).let {
                (it.newState as AppNotInitialized).let { newState ->
                    assertEquals(result, newState.pendingDeeplinkResult)
                    assertEquals(0, it.effects.size)
                }
            }
        }
    }

    @Test
    fun `DeeplinkCheckTimeout - AppInitialized`() {
        appInitialized(viewState = notSplashScreenViewState()).let { oldState ->
            deeplinkReducer().reduce(DeeplinkCheckTimeoutAction, oldState).let {
                assertEquals(oldState, it.newState)
                assertEquals(1, it.effects.size)
                assertEquals(AppActionEffect(DeeplinkCheckedAction(NoDeeplink)), it.effects.first())
            }
        }

    }

    companion object {
        fun deeplinkReducer(): DeeplinkReducer {
            return DeeplinkReducer(loginReducer())
        }

        fun anyDeeplinkResult(): List<DeeplinkResult> {
            return listOf(
                NoDeeplink,
                DeeplinkError(Exception(), null),
                validDeeplinkParams()
            )
        }

        fun notSplashScreenViewState(): AppViewState {
            return SignInScreenView
        }
    }

}
