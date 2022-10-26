package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.assertHasEffect
import com.hypertrack.android.assertWithChecks
import com.hypertrack.android.createEffectCheck
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppActionEffect
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appInitialized
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.LoginAppAction
import com.hypertrack.android.interactors.app.NavigateAppEffect
import com.hypertrack.android.interactors.app.ShowAppMessageEffect
import com.hypertrack.android.interactors.app.effect.navigation.NavigateToUserScopeScreensEffect
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppViewStateTest.Companion.tabsView
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.models.local.DeviceId
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class SignedInActionTest {

    private val deviceId = DeviceId("test")

    @Test
    fun `SignedInAction - Initialized (SplashScreenState, UserLoggedIn)`() {
        val oldUserScope = mockk<UserScope>()
        val newUserScope = mockk<UserScope>()
        val state = appInitialized(
            userState = userLoggedIn(
                userScope = oldUserScope,
                deviceId = deviceId
            ),
            viewState = SplashScreenView
        )
        val newUserState = userLoggedIn(
            userScope = newUserScope,
            deviceId = deviceId
        )
        val action = SignedInAction(newUserState)
        appReducer().reduce(state, LoginAppAction(action)).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertEquals(newUserState, newState.userState)
                result.effects.assertWithChecks(
                    { it.assertHasEffect(ShowAppMessageEffect::class) },
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    },
                    createEffectCheck<AppActionEffect> {
                        it.action is HistoryAppAction &&
                                (it.action as HistoryAppAction)
                                    .historyAction is StartDayHistoryLoadingAction
                    }
                )
            }
        }
    }

    @Test
    fun `SignedInAction - Initialized (UserScopeScreensState, UserLoggedIn)`() {
        val oldUserScope = mockk<UserScope>()
        val newUserScope = mockk<UserScope>()
        val state = appInitialized(
            userState = userLoggedIn(
                userScope = oldUserScope,
                deviceId = deviceId
            ),
            viewState = tabsView()
        )
        val newUserState = userLoggedIn(
            userScope = newUserScope,
            deviceId = deviceId
        )
        val action = SignedInAction(newUserState)
        appReducer().reduce(state, LoginAppAction(action)).let { result ->
            (result.newState as AppInitialized).let { newState ->
                assertEquals(newUserState, newState.userState)
                result.effects.assertWithChecks(
                    { it.assertHasEffect(ShowAppMessageEffect::class) },
                    createEffectCheck<NavigateAppEffect> {
                        it.navigationEffect is NavigateToUserScopeScreensEffect
                    },
                    createEffectCheck<AppActionEffect> {
                        it.action is HistoryAppAction &&
                                (it.action as HistoryAppAction)
                                    .historyAction is StartDayHistoryLoadingAction
                    }
                )
            }
        }
    }

}
