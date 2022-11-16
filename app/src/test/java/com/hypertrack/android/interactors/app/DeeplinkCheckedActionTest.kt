package com.hypertrack.android.interactors.app

import com.hypertrack.android.assertHasEffect
import com.hypertrack.android.assertWithChecks
import com.hypertrack.android.createEffectCheck
import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppEffectTest.Companion.assertContainsNavToSignIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appNotInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.reducer.DeeplinkReducerTest.Companion.anyDeeplinkResult
import io.mockk.mockk
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppViewStateTest.Companion.tabsView
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.utils.map
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.Exception

// todo replace appReducer with deeplinkReducer
class DeeplinkCheckedActionTest {

    // DeeplinkCheckedAction
    // DeeplinkCheckedAction(valid deeplink)
    // DeeplinkCheckedAction(no deeplink)
    // DeeplinkCheckedAction(error)
    // + (branch connection error)
    // AppNotInitialized
    // AppNotInitialized(pending deeplink)
    // AppInitialized(view, user)
    // view = SplashScreenView, SignInView, CurrentTripScreenView
    // user = UserLoggedIn, UserNotLoggedIn

    @Test
    fun `DeeplinkCheckedAction - AppNotInitialized`() {
        anyDeeplinkResult().forEach { deeplinkResult ->
            val state = appNotInitialized()
            val action = DeeplinkCheckedAction(deeplinkResult)
            appReducer().reduce(state, action).let { result ->
                (result.newState as AppNotInitialized).let {
                    assertEquals(deeplinkResult, it.pendingDeeplinkResult)
                    result.effects.assertWithChecks(
                        createStopDeeplinkTimeoutTimerCheck(),
                    )
                }
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(valid deeplink) - AppNotInitialized(pending deeplink)`() {
        val state = appNotInitialized(pendingDeeplinkResult = validDeeplinkParams())
        val actionDeeplinkParams = validDeeplinkParams(email = "test@mail.com")
        val action = DeeplinkCheckedAction(actionDeeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppNotInitialized).let {
                assertEquals(actionDeeplinkParams, it.pendingDeeplinkResult)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(valid deeplink) - AppInitialized(SplashScreenView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val deeplinkParams = validDeeplinkParams()
        val action = DeeplinkCheckedAction(deeplinkParams)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(LoginWithDeeplinkEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(error) - AppInitialized(SplashScreenView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    createNavigateToSignInCheck(),
                    { it.assertHasEffect(ShowAndReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(no deeplink) - AppInitialized(SplashScreenView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    createNavigateToSignInCheck(),
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(error) - AppInitialized(SplashScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(ShowAndReportAppErrorEffect::class) },
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(no deeplink) - AppInitialized(SplashScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(error) - AppInitialized(SignInView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SignInScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(ShowAndReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(no deeplink) - AppInitialized(SignInView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SignInScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let {
                assertFalse(it.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(valid deeplink) - AppInitialized(CurrentTripScreenView, UserLoggedIn)`() {
        val state = appInitialized(
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
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(LoginWithDeeplinkEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(error) - AppInitialized(CurrentTripScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction()
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(ShowAndReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(branch connection error) - AppInitialized(SplashScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(ReportAppErrorEffect::class) },
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(branch connection error) - AppInitialized(CurrentTripScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    { it.assertHasEffect(ReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(branch connection error) - AppInitialized(SplashScreenView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SplashScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createStopDeeplinkTimeoutTimerCheck(),
                    createNavigateToSignInCheck(),
                    { it.assertHasEffect(ReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(branch connection error) - AppInitialized(SignInView, UserNotLoggedIn)`() {
        val state = appInitialized(
            userState = UserNotLoggedIn,
            viewState = SignInScreenView,
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = deeplinkCheckedErrorAction(BranchErrorException(-113, ""))
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createEffectCheck<StopTimer> { it.timer is DeeplinkCheckTimeoutTimer },
                    { it.assertHasEffect(ReportAppErrorEffect::class) }
                )
            }
        }
    }

    @Test
    fun `DeeplinkCheckedAction(no deeplink) - AppInitialized(CurrentTripScreenView, UserLoggedIn)`() {
        val state = appInitialized(
            userState = userLoggedIn(),
            viewState = tabsView(),
            // by DeeplinkCheckStartedAction
            showProgressbar = true
        )
        val action = DeeplinkCheckedAction(NoDeeplink)
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertFalse(newState.showProgressbar)
                result.effects.assertWithChecks(
                    createEffectCheck<StopTimer> { it.timer is DeeplinkCheckTimeoutTimer }
                )
            }
        }
    }

    companion object {
        fun deeplinkCheckedAction(deeplinkResult: DeeplinkResult): DeeplinkCheckedAction {
            return DeeplinkCheckedAction(deeplinkResult)
        }

        fun deeplinkCheckedErrorAction(
            exception: Exception = Exception()
        ): DeeplinkCheckedAction {
            return DeeplinkCheckedAction(DeeplinkError(exception, mockk()))
        }

        fun createStopDeeplinkTimeoutTimerCheck(): (Set<Any>) -> Unit {
            return createEffectCheck<StopTimer> { it.timer is DeeplinkCheckTimeoutTimer }
        }

        fun createNavigateToSignInCheck(): (Set<Any>) -> Unit {
            return { effects -> assertContainsNavToSignIn(effects.map { it as AppEffect }) }
        }
    }

}
