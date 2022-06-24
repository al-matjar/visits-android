package com.hypertrack.android.interactors.app

import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.createdState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.initializedState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import io.mockk.mockk
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
            (result.newState as NotInitialized).let {
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
            (result.newState as NotInitialized).let {
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
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 1)
                result.effects.filterIsInstance<LoginWithDeeplinkEffect>().first()
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 2)
                assertTrue(
                    result.effects
                        .filterIsInstance<NavigateToSignInEffect>()
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
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertTrue(result.effects.size == 1)
                assertTrue(result.effects.first() is NavigateToSignInEffect)
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenState, UserLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
    fun `Initialized (SplashScreenState, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.isEmpty())
            }
        }
    }

    @Test
    fun `Initialized (UserScopeScreensState, UserLoggedIn) - DeeplinkCheckedAction (valid deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                assertTrue(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                result.effects.filterIsInstance<LoginWithDeeplinkEffect>().first()
            }
        }
    }

    @Test
    fun `Initialized (UserScopeScreensState, UserLoggedIn) - DeeplinkCheckedAction (error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
    fun `Initialized (SplashScreenState, UserLoggedIn) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
    fun `Initialized (UserLoggedIn, UserScopeScreensState) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
                        .filterIsInstance<NavigateToSignInEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (UserNotLoggedIn, SignInState) - DeeplinkCheckedAction (branch connection error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SignInState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
    fun `Initialized (UserScopeScreensState, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
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
