package com.hypertrack.android.interactors.app

import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppEffectTest.Companion.assertNavToSignIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.createdState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.initializedState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import io.mockk.mockk
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppViewStateTest.Companion.tabsView
import com.hypertrack.android.interactors.app.state.CurrentTripTab
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.logistics.android.github.NavGraphDirections
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.Exception

class DeeplinkCheckedActionTest {

    @Test
    fun `Created - DeeplinkCheckedAction`() {
        val state = createdState()
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppNotInitialized).let {
                println(result.effects)
                assertEquals(deeplinkParams, it.pendingDeeplinkResult)
                assertTrue(result.effects.isEmpty())
            }
        }
    }

    @Test
    fun `Created (pending deeplink) - DeeplinkCheckedAction`() {
        val state = createdState(pendingDeeplinkResult = validDeeplinkParams())
        val deeplinkParams = validDeeplinkParams(email = "test@mail.com")
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppNotInitialized).let {
                println(result.effects)
                assertEquals(deeplinkParams, it.pendingDeeplinkResult)
                assertTrue(result.effects.isEmpty())
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (valid deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 1)
                result.effects.filterIsInstance<LoginWithDeeplinkEffect>().first()
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserNotLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 2)
                assertNavToSignIn(result.effects)
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserNotLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 1)
                assertNavToSignIn(result.effects)
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 2)
                assertTrue(
                    result.effects
                        .filterIsInstance<NavigateToUserScopeScreensEffect>()
                        .isNotEmpty()
                )
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 1)
                assertTrue(result.effects.first() is NavigateToUserScopeScreensEffect)
            }
        }
    }

    @Test
    fun `Initialized (SignInState, UserNotLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SignInState, UserNotLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.isEmpty())
            }
        }
    }

    @Test
    fun `Initialized (CurrentTripScreenView, UserLoggedIn) - DeeplinkCheckedAction (valid deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertTrue(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                result.effects.filterIsInstance<LoginWithDeeplinkEffect>().first()
            }
        }
    }

    @Test
    fun `Initialized (CurrentTripScreenView, UserLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserLoggedIn) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 2)
                assertTrue(
                    result.effects
                        .filterIsInstance<ReportAppErrorEffect>()
                        .isNotEmpty()
                )
                assertTrue(
                    result.effects
                        .filterIsInstance<NavigateToUserScopeScreensEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (UserLoggedIn, CurrentTripScreenView) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenView, UserNotLoggedIn) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 2)
                assertTrue(
                    result.effects
                        .filterIsInstance<ReportAppErrorEffect>()
                        .isNotEmpty()
                )
                assertNavToSignIn(result.effects)
            }
        }
    }

    @Test
    fun `Initialized (UserNotLoggedIn, SignInState) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SignInScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<ShowAndReportAppErrorEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (CurrentTripScreenView, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertFalse(it.showProgressbar)
                println(result.effects)
                assertTrue(result.effects.isEmpty())
            }
        }
    }

    companion object {
        fun deeplinkCheckedErrorAction(
            exception: Exception = Exception()
        ): DeeplinkCheckedAction {
            return DeeplinkCheckedAction(DeeplinkError(exception, mockk()))
        }
    }

}
