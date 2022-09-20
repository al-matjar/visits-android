package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.interactors.app.AppEffectTest
import com.hypertrack.android.interactors.app.AppEffectTest.Companion.assertNavToSignIn
import com.hypertrack.android.interactors.app.AppReducerTest
import com.hypertrack.android.interactors.app.LoginAppAction
import com.hypertrack.android.interactors.app.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import junit.framework.TestCase
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.lang.Exception

class LoginErrorActionTest {

    @Test
    fun `LoginErrorAction - Initialized (SplashScreenState, UserLoggedIn)`() {
        val state = AppReducerTest.appInitialized(
            userState = AppReducerTest.userLoggedIn(),
            viewState = SplashScreenView
        ).copy(
            showProgressbar = true
        )
        val action = LoginErrorAction(Exception())
        AppReducerTest.appReducer().reduce(state, LoginAppAction(action)).let { result ->
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
    fun `LoginErrorAction - Initialized (SplashScreenState, UserNotLoggedIn)`() {
        val state = AppReducerTest.appInitialized(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView
        ).copy(
            showProgressbar = true
        )
        val action = LoginErrorAction(Exception())
        AppReducerTest.appReducer().reduce(state, LoginAppAction(action)).let { result ->
            (result.newState as AppInitialized).let {
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 2)
                assertNavToSignIn(result.effects)
                result.effects.filterIsInstance<ShowAndReportAppErrorEffect>().first()
            }
        }
    }

}
