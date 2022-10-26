package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.TestInjector
import com.hypertrack.android.assertHasEffect
import com.hypertrack.android.assertWithChecks
import com.hypertrack.android.createEffectCheck
import com.hypertrack.android.interactors.app.AppEffectTest.Companion.assertContainsNavToSignIn
import com.hypertrack.android.interactors.app.AppReducerTest
import com.hypertrack.android.interactors.app.LoginAppAction
import com.hypertrack.android.interactors.app.LoginWithPublishableKey
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigationEffect
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
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
            (result.newState as AppInitialized).let { newState ->
                println(result)
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    { it.assertHasEffect(ShowAndReportAppErrorEffect::class) },
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    }
                )
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
                assertContainsNavToSignIn(result.effects)
                result.effects.filterIsInstance<ShowAndReportAppErrorEffect>().first()
            }
        }
    }

}
