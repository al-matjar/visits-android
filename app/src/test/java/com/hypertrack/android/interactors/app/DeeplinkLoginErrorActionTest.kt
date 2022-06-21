package com.hypertrack.android.interactors.app

import com.hypertrack.android.interactors.app.AppEffectTest.Companion.assertNavToSignIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.initializedState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.lang.Exception

class DeeplinkLoginErrorActionTest {

    @Test
    fun `DeeplinkLoginErrorAction - Initialized (SplashScreenState, UserLoggedIn)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenView
        ).copy(
            showProgressbar = true
        )
        val action = DeeplinkLoginErrorAction(Exception())
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 2)
                result.effects.filterIsInstance<NavigateToUserScopeScreensEffect>().first()
                result.effects.filterIsInstance<ShowAndReportAppErrorEffect>().first()
            }
        }
    }

    @Test
    fun `DeeplinkLoginErrorAction - Initialized (SplashScreenState, UserNotLoggedIn)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView
        ).copy(
            showProgressbar = true
        )
        val action = DeeplinkLoginErrorAction(Exception())
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 2)
                assertNavToSignIn(result.effects)
                result.effects.filterIsInstance<ShowAndReportAppErrorEffect>().first()
            }
        }
    }

}
