package com.hypertrack.android.interactors.app

import com.hypertrack.android.interactors.app.effect.navigation.NavigateToSignInEffect
import com.hypertrack.logistics.android.github.NavGraphDirections
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

class AppEffectTest {
    companion object {
        fun assertContainsNavToSignIn(effects: Set<AppEffect>) {
            effects.filterIsInstance<NavigateAppEffect>().also {
                assertEquals(1, it.size)
            }.first().let {
                assertTrue(
                    it.navigationEffect is NavigateToSignInEffect
                )
            }
        }
    }
}
