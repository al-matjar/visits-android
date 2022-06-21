package com.hypertrack.android.interactors.app

import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.initializedState
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppViewStateTest.Companion.tabsView
import com.hypertrack.android.interactors.app.state.CurrentTripTab
import com.hypertrack.android.interactors.app.state.SplashScreenView
import com.hypertrack.android.models.local.DeviceId
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class SignedInActionTest {

    private val deviceId = DeviceId("test")

    @Test
    fun `SignedInAction - Initialized (SplashScreenState, UserLoggedIn)`() {
        val oldUserScope = mockk<UserScope>()
        val newUserScope = mockk<UserScope>()
        val state = initializedState(
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
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                println(result.effects)
                assertEquals(newUserState, newState.userState)
                assertTrue(result.effects.size == 2)
                result.effects
                    .filterIsInstance<CleanupUserScopeEffect>()
                    .first()
                    .let {
                        assertEquals(oldUserScope, it.oldUserScope)
                    }
                result.effects
                    .filterIsInstance<NavigateToUserScopeScreensEffect>()
                    .first()
                    .let {
                        assertEquals(newUserState, it.newUserState)
                    }
            }
        }
    }

    @Test
    fun `SignedInAction - Initialized (UserScopeScreensState, UserLoggedIn)`() {
        val oldUserScope = mockk<UserScope>()
        val newUserScope = mockk<UserScope>()
        val state = initializedState(
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
        appReducer().reduce(state, action).let { result ->
            (result.newState as AppInitialized).let { newState ->
                println(result.effects)
                assertEquals(newUserState, newState.userState)
                assertTrue(result.effects.size == 2)
                result.effects
                    .filterIsInstance<CleanupUserScopeEffect>()
                    .first()
                    .let {
                        assertEquals(oldUserScope, it.oldUserScope)
                    }
                result.effects
                    .filterIsInstance<NavigateToUserScopeScreensEffect>()
                    .first()
                    .let {
                        assertEquals(newUserState, it.newUserState)
                    }
            }
        }
    }

}
