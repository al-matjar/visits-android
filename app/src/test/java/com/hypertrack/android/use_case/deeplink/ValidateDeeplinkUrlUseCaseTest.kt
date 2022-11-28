package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.ui.screens.sign_in.use_case.HandlePastedDeeplinkUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase.Companion.createDeeplinkRegex
import junit.framework.TestCase
import org.junit.Test

class ValidateDeeplinkUrlUseCaseTest {

    @Test
    fun `it should correctly match deeplink pattern`() {
        listOf(
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?ddddddd",
            "https://hypertrack-logistics.app.link/1oF",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgddddddddddddddd",
        ).forEach { link ->
            createDeeplinkRegex().matcher(link).matches().let {
                println(it)
                TestCase.assertTrue(it)
            }
        }

        listOf(
            "https://hypertrack-logistics.app.link/",
            "https://google.com/1oF0VcDvYgddddddddddddddd"
        ).forEach { link ->
            createDeeplinkRegex().matcher(link).matches().let {
                println(it)
                TestCase.assertFalse(it)
            }
        }
    }

    companion object {
        fun validateDeeplinkUrlUseCase() = ValidateDeeplinkUrlUseCase(createDeeplinkRegex())
    }

}
