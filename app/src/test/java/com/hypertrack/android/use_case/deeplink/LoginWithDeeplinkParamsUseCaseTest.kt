package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.interactors.app.UserAuthDataTest.Companion.emailAuthData
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCaseTest.Companion.logExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.asSuccess
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoginWithDeeplinkParamsUseCaseTest {

    @Test
    fun `it should login user with valid deeplink params`() {
        runBlocking {
            val publishableKey = "abcd"
            val deeplinkParams = validDeeplinkParams(
                publishableKey = publishableKey
            )
            loginWithDeeplinkParamsUseCase(deeplinkParams).execute(
                deeplinkParams
            ).collect { result ->
                (result as AbstractSuccess).let {
                    assertEquals(publishableKey, it.success.publishableKey.value)
                }
            }
        }
    }

    companion object {
        fun loginWithDeeplinkParamsUseCase(
            deeplinkParams: DeeplinkParams
        ): LoginWithDeeplinkParamsUseCase {
            return LoginWithDeeplinkParamsUseCase(
                getConfiguredHypertrackSdkInstanceUseCase = mockk {
                    every { execute(any()) } returns flowOf(mockk())
                },
                logExceptionToCrashlyticsUseCase = logExceptionToCrashlyticsUseCase(),
                loginWithPublishableKeyUseCase = mockk {
                    every { execute(any(), any(), any(), any()) } answers {
                        flowOf(
                            LoggedIn(
                                hyperTrackSdk = mockk(),
                                publishableKey = thirdArg()
                            ).asSuccess()
                        )
                    }
                },
                validateDeeplinkUseCase = mockk {
                    every { execute(deeplinkParams) } returns flowOf(DeeplinkValid(
                        emailAuthData(
                            publishableKey = deeplinkParams.parameters["publishable_key"].let {
                                RealPublishableKey(it as String)
                            }
                        ),
                        null
                    ))
                }
            )
        }
    }

}
