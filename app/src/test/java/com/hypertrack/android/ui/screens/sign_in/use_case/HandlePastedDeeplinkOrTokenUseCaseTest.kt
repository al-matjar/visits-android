package com.hypertrack.android.ui.screens.sign_in.use_case

import android.app.Activity
import com.hypertrack.android.TestInjector
import com.hypertrack.android.TestInjector.TEST_PUBLISHABLE_KEY
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCaseTest.Companion.logExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCaseTest.Companion.stubLogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogMessageToCrashlyticsUseCaseTest.Companion.testLogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.DeeplinkValidationError
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.toFlow
import com.hypertrack.sdk.HyperTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HandlePastedDeeplinkOrTokenUseCaseTest {

    @Test
    fun `it should correctly handle login token`() {
        val map = mapOf(
            "data" to mapOf(
                "publishable_key" to "pk",
                "email" to "email",
                "metadata" to mapOf(
                    "field" to "value"
                )
            )
        )

        runBlocking {
            val sdk = mockk<HyperTrack>()
            val logExceptionToCrashlyticsUseCase = stubLogExceptionToCrashlyticsUseCase()

            handlePastedDeeplinkOrTokenUseCase(
                sdk = sdk,
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase
            ).execute(
                activity = mockActivity(),
                text = pseudoToBase64(map)
            ).collect {
                assertEquals(
                    AbstractSuccess<LoggedIn, DeeplinkValidationError>(
                        LoggedIn(
                            sdk,
                            TEST_PUBLISHABLE_KEY
                        )
                    ), it
                )
                // it should log exception on each deeplink or token pasted
                verify { logExceptionToCrashlyticsUseCase.execute(any(), any()) }
            }
        }
    }

    @Test
    fun `it should correctly handle pasted deeplink`() {
        runBlocking {
            val sdk = mockk<HyperTrack>()
            val logExceptionToCrashlyticsUseCase = stubLogExceptionToCrashlyticsUseCase()

            handlePastedDeeplinkOrTokenUseCase(
                sdk = sdk,
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase
            ).execute(
                activity = mockActivity(),
                text = TEST_DEEPLINK
            ).collect {
                assertEquals(
                    AbstractSuccess<LoggedIn, DeeplinkValidationError>(
                        LoggedIn(
                            sdk,
                            TEST_PUBLISHABLE_KEY
                        )
                    ), it
                )
                // it should log exception on each deeplink or token pasted
                verify { logExceptionToCrashlyticsUseCase.execute(any(), any()) }
            }
        }
    }

    @Test
    fun `it should correctly match deeplink pattern`() {
        listOf(
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgb?ddddddd",
            "https://hypertrack-logistics.app.link/1oF",
            "https://hypertrack-logistics.app.link/1oF0VcDvYgddddddddddddddd",
        ).forEach { link ->
            HandlePastedDeeplinkOrTokenUseCase.DEEPLINK_REGEX.matcher(link).matches().let {
                println(it)
                assertTrue(it)
            }
        }

        listOf(
            "https://hypertrack-logistics.app.link/",
            "https://google.com/1oF0VcDvYgddddddddddddddd"
        ).forEach { link ->
            HandlePastedDeeplinkOrTokenUseCase.DEEPLINK_REGEX.matcher(link).matches().let {
                println(it)
                assertFalse(it)
            }
        }
    }


    companion object {
        private const val TEST_DEEPLINK = "https://hypertrack-logistics.app.link/1oF0VcDvYgb"

        fun handlePastedDeeplinkOrTokenUseCase(
            sdk: HyperTrack,
            logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase? = null
        ): HandlePastedDeeplinkOrTokenUseCase {
            return HandlePastedDeeplinkOrTokenUseCase(
                loginWithDeeplinkParamsUseCase = mockk {
                    every { execute(any()) } returns AbstractSuccess<LoggedIn, DeeplinkValidationError>(
                        LoggedIn(sdk, TEST_PUBLISHABLE_KEY)
                    ).toFlow()
                },
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase
                    ?: logExceptionToCrashlyticsUseCase(),
                logMessageToCrashlyticsUseCase = testLogMessageToCrashlyticsUseCase(),
                osUtilsProvider = mockk {
                    every { parseUri(any()) } returns mockk()
                    every { decodeBase64(any()) } answers {
                        pseudoFromBase64(firstArg())
                    }
                },
                branchWrapper = mockk {
                    every { handleGenericDeeplink(any(), any(), any(), any()) } answers {
                        arg<(DeeplinkResult) -> Unit>(3).invoke(validDeeplinkParams())
                    }
                },
                moshi = TestInjector.getMoshi(),
            )
        }

        fun mockActivity(): Activity {
            return mockk {
                every { intent } returns mockk()
            }
        }

        private fun pseudoToBase64(map: Map<String, Any>): String {
            return TestInjector.getMoshi().createAnyMapAdapter().toJson(map).let { "%%%$it%%%" }
        }

        private fun pseudoFromBase64(it: String): String {
            return it.replace("%%%", "").replace("%%%", "")
        }
    }
}
