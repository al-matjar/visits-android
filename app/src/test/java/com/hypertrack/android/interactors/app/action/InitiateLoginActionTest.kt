package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.TestInjector.TEST_PUBLISHABLE_KEY
import com.hypertrack.android.TestInjector.TEST_USER_DATA
import com.hypertrack.android.assertHasEffect
import com.hypertrack.android.assertEffects
import com.hypertrack.android.assertWithChecks
import com.hypertrack.android.createEffectCheck
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.LoginWithPublishableKey
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.reducer.login.AlreadyLoggedInException
import com.hypertrack.android.interactors.app.reducer.login.LoginAlreadyInProgressException
import com.hypertrack.android.interactors.app.reducer.login.LoginReducerTest.Companion.loginReducer
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppViewState
import com.hypertrack.android.interactors.app.state.AppViewStateTest.Companion.tabsView
import com.hypertrack.android.interactors.app.state.SignInScreenView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.models.local.Email
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.state_machine.ReducerResult
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

@Suppress("SimpleRedundantLet")
class InitiateLoginActionTest {

    // InitiateLoginAction on SplashScreen should end with either LoginWithPublishableKey
    // or Navigate effects to proceed from the Splash screen
    // It should hide progressbar if there is no LoginWithPublishableKey effect

    @Test
    fun `InitiateLoginAction - SplashScreen(not logged in)`() {
        test(
            viewState = SplashScreenView
        ).let { result ->
            assertTrue(result.newState.showProgressbar)
            result.effects.assertEffects(
                LoginWithPublishableKey::class
            )
        }
    }

    @Test
    fun `InitiateLoginAction - SplashScreen(already logged in)`() {
        test(
            viewState = SplashScreenView,
            oldUserData = TEST_USER_DATA.copy(email = Email("test@mail.com"))
        ).let { result ->
            assertTrue(result.newState.showProgressbar)
            result.effects.assertWithChecks(
                createEffectCheck<LoginWithPublishableKey> {
                    it.userData == TEST_USER_DATA
                }
            )
        }
    }

    @Test
    fun `InitiateLoginAction - Any(login in progress)`() {
        listOf(
            SplashScreenView,
            SignInScreenView,
            tabsView()
        ).forEach { viewState ->
            test(
                viewState = viewState,
                loginInProgress = true
            ).let { result ->
                assertFalse(result.newState.showProgressbar)
                result.effects.assertWithChecks(
                    createEffectCheck<ShowAndReportAppErrorEffect> {
                        it.exception is LoginAlreadyInProgressException
                    }
                )
            }
        }
    }

    @Test
    fun `InitiateLoginAction SplashScreen(logged in with same user)`() {
        test(
            viewState = SplashScreenView,
            oldUserData = TEST_USER_DATA
        ).let { result ->
            assertFalse(result.newState.showProgressbar)
            result.effects.assertWithChecks(
                createEffectCheck<ShowAndReportAppErrorEffect> {
                    it.exception is AlreadyLoggedInException
                },
                createEffectCheck<NavigateAppEffect> {
                    it.navigationEffect is NavigateToUserScopeScreensEffect
                }
            )
        }
    }

    @Test
    fun `InitiateLoginAction - UserScopeScreens(already logged in)`() {
        test(
            viewState = tabsView(),
            oldUserData = TEST_USER_DATA.copy(email = Email("test@mail.com"))
        ).let { result ->
            assertTrue(result.newState.showProgressbar)
            result.effects.assertWithChecks(
                createEffectCheck<LoginWithPublishableKey> {
                    it.userData == TEST_USER_DATA
                }
            )
        }
    }

    @Test
    fun `InitiateLoginAction - UserScopeScreens(logged in with same user)`() {
        test(
            viewState = tabsView(),
            oldUserData = TEST_USER_DATA
        ).let { result ->
            assertFalse(result.newState.showProgressbar)
            result.effects.assertWithChecks(
                createEffectCheck<ShowAndReportAppErrorEffect> {
                    it.exception is AlreadyLoggedInException
                }
            )
        }
    }

    @Test
    fun `InitiateLoginAction - SignInScreen(not logged in)`() {
        test(
            viewState = SignInScreenView
        ).let { result ->
            assertTrue(result.newState.showProgressbar)
            result.effects.assertEffects(
                LoginWithPublishableKey::class
            )
        }
    }

    companion object {
        private fun test(
            viewState: AppViewState,
            oldUserData: UserData? = null,
            loginInProgress: Boolean = false
        ): ReducerResult<out AppInitialized, out AppEffect> {
            return loginReducer().reduce(
                InitiateLoginAction(
                    TEST_PUBLISHABLE_KEY,
                    TEST_USER_DATA
                ),
                appInitialized(
                    userState = userLoggedIn(userData = oldUserData),
                    viewState = viewState,
                    userIsLoggingIn = if (loginInProgress) TEST_USER_DATA else null
                )
            )
        }

    }

}
