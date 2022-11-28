package com.hypertrack.android.ui.screens.sign_in.use_case

import android.app.Activity
import com.hypertrack.android.TestInjector
import com.hypertrack.android.TestInjector.TEST_PUBLISHABLE_KEY
import com.hypertrack.android.TestInjector.TEST_USER_DATA
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.interactors.app.action.InitiateLoginAction
import com.hypertrack.android.ui.screens.sign_in.use_case.result.PasteSuccess
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCaseTest.Companion.logExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCaseTest.Companion.stubLogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogMessageToCrashlyticsUseCaseTest.Companion.testLogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCaseTest
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCaseTest.Companion.validateDeeplinkUrlUseCase
import com.hypertrack.android.use_case.deeplink.result.DeeplinkParamsInvalid
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.toFlow
import com.hypertrack.sdk.HyperTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HandlePastedDeeplinkUseCaseTest {

    @Test
    fun `it should correctly handle pasted deeplink`() {
        runBlocking {
            val sdk = mockk<HyperTrack>()
            val logExceptionToCrashlyticsUseCase = stubLogExceptionToCrashlyticsUseCase()

            handlePastedDeeplinkOrTokenUseCase(
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase
            ).execute(
                text = TEST_DEEPLINK
            ).collect {
                assertEquals(
                    PasteSuccess(
                        InitiateLoginAction(
                            TEST_PUBLISHABLE_KEY,
                            TEST_USER_DATA
                        )
                    ).asSuccess(),
                    it
                )
                // it should log exception on each deeplink or token pasted
                verify { logExceptionToCrashlyticsUseCase.execute(any(), any()) }
            }
        }
    }




    companion object {
        private const val TEST_DEEPLINK = "https://hypertrack-logistics.app.link/1oF0VcDvYgb"

        fun handlePastedDeeplinkOrTokenUseCase(
            logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase? = null
        ): HandlePastedDeeplinkUseCase {
            return HandlePastedDeeplinkUseCase(
                validateDeeplinkUrlUseCase = validateDeeplinkUrlUseCase(),
                loginWithDeeplinkParamsUseCase = mockk {
                    every { execute(any()) } returns AbstractSuccess<InitiateLoginAction, DeeplinkParamsInvalid>(
                        InitiateLoginAction(TEST_PUBLISHABLE_KEY, TEST_USER_DATA)
                    ).asSuccess().toFlow()
                },
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase
                    ?: logExceptionToCrashlyticsUseCase(),
                logMessageToCrashlyticsUseCase = testLogMessageToCrashlyticsUseCase(),
                getBranchDataFromAppBackendUseCase = mockk {
                    every { execute(any()) } returns validDeeplinkParams().asSuccess().toFlow()
                }
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
