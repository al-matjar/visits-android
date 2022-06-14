package com.hypertrack.android.interactors.app

import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.createdState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.initializedState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.lang.Exception

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
            viewState = SplashScreenState
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
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (deeplink error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenState
        )
        val action = DeeplinkCheckedAction(DeeplinkError(Exception()))
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
                        .filterIsInstance<HandleAppErrorMessageEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenState, UserNotLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = SplashScreenState
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
    fun `Initialized (SplashScreenState, UserLoggedIn) - DeeplinkCheckedAction (deeplink error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenState
        )
        val action = DeeplinkCheckedAction(DeeplinkError(Exception()))
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
                        .filterIsInstance<HandleAppErrorMessageEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SplashScreenState, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = SplashScreenState
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
    fun `Initialized (SignInState, UserNotLoggedIn) - DeeplinkCheckedAction (deeplink error)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = UserScopeScreensState
        )
        val action = DeeplinkCheckedAction(DeeplinkError(Exception()))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<HandleAppErrorMessageEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (SignInState, UserNotLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = UserNotLoggedIn,
            viewState = UserScopeScreensState
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
            viewState = UserScopeScreensState
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
    fun `Initialized (UserScopeScreensState, UserLoggedIn) - DeeplinkCheckedAction (deeplink error)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState
        )
        val action = DeeplinkCheckedAction(DeeplinkError(Exception()))
        appReducer().reduce(state, action).let { result ->
            (result.newState as Initialized).let {
                println(result.effects)
                assertFalse(it.showProgressbar)
                assertTrue(result.effects.size == 1)
                assertTrue(
                    result.effects
                        .filterIsInstance<HandleAppErrorMessageEffect>()
                        .isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `Initialized (UserScopeScreensState, UserLoggedIn) - DeeplinkCheckedAction (no deeplink)`() {
        val state = initializedState(
            userState = userLoggedIn(),
            viewState = UserScopeScreensState
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

}
