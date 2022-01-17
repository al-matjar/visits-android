package com.hypertrack.android.view_models

import android.app.Activity
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.utils.*
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test

class SignInViewModelTest {

    @Test
    fun `it should correctly handle login token`() {
        val adapter = Injector.getMoshi().createAnyMapAdapter()

        fun pseudoToBase64(it: String): String {
            return "%%%$it%%%"
        }

        fun pseudoFromBase64(it: String): String {
            return it.replace("%%%", "").replace("%%%", "")
        }

        val map = mapOf(
            "publishable_key" to "pk",
            "email" to "email",
            "metadata" to mapOf(
                "field" to "value"
            )
        )

        val vm = object : SignInViewModel(
            mockk(relaxed = true) {
                every { osUtilsProvider } returns mockk(relaxed = true) {
                    every { decodeBase64(any()) } answers { pseudoFromBase64(firstArg()) }
                }
            },
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        ) {
            override fun onDeeplinkParamsReceived(
                deeplinkResult: DeeplinkResult,
                activity: Activity
            ) {
                assertEquals(map, (deeplinkResult as DeeplinkParams).parameters)
            }
        }

        vm.handleDeeplinkOrToken(
            map.let { adapter.toJson(it) }.let { pseudoToBase64(it) },
            mockk(relaxed = true)
        )

        vm.handleDeeplinkOrToken(
            map.let { adapter.toJson(it) }.let { pseudoToBase64(it) }.let {
                "$it?test"
            },
            mockk(relaxed = true)
        )

        vm.handleDeeplinkOrToken(
            map.let { adapter.toJson(it) }.let { pseudoToBase64(it) }.let {
                "$it?"
            },
            mockk(relaxed = true)
        )
    }

}