package com.hypertrack.android.interactors.app

import com.hypertrack.logistics.android.github.NavGraphDirections
import junit.framework.TestCase

class AppEffectTest {
    companion object {
        fun assertNavToSignIn(effects: Set<AppEffect>) {
            effects.filterIsInstance<NavigateEffect>().also {
                TestCase.assertEquals(1, it.size)
            }.first().let {
                TestCase.assertEquals(
                    NavGraphDirections.actionGlobalSignInFragment(),
                    it.destination
                )
            }
        }
    }
}
