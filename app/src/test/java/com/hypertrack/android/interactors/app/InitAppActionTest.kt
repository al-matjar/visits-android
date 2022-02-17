package com.hypertrack.android.interactors.app

import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.appReducer
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.createdState
import io.mockk.mockk
import junit.framework.TestCase
import org.junit.Test

class InitAppActionTest {

    @Test
    fun `InitAppAction - Created`() {
        val state = createdState()
        val action = InitAppAction(
            application = mockk()
        )
        appReducer().reduce(state, action).let {
            TestCase.assertEquals(state, it.newState)
            TestCase.assertTrue(it.effects.size == 1)
            TestCase.assertTrue(it.effects.first() is InitAppEffect)
            // sends AppInitializedAction
        }
    }

    @Test
    fun `InitAppAction - Created (pending deeplink)`() {
        val state = createdState(pendingDeeplinkResult = NoDeeplink)
        val action = InitAppAction(
            application = mockk()
        )
        appReducer().reduce(state, action).let {
            TestCase.assertEquals(state, it.newState)
            TestCase.assertTrue(it.effects.size == 1)
            TestCase.assertTrue(it.effects.first() is InitAppEffect)
            // sends AppInitializedAction
        }
    }

}
