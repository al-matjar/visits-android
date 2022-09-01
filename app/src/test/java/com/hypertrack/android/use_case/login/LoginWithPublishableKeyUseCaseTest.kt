package com.hypertrack.android.use_case.login

import com.hypertrack.android.TestInjector
import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.accessTokenRepository
import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.UserAuthDataTest.Companion.emailAuthData
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.repository.preferences.SharedPreferencesStringEntry
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoginWithPublishableKeyUseCaseTest {

    @Suppress("DEPRECATION")
    @Test
    fun `it should login user, save access token and set device info`() {
        runBlocking {
            val metadata = mapOf(
                "swords" to "rust",
                "heart" to "dust"
            )
            val email = Email("my mail")
            val deviceNameSlot = slot<String>()
            val deviceMetadataSlot = slot<Map<String, Any>>()

            val mockUserDataSetting = mockk<SharedPreferencesStringEntry<UserData>> {
                every { save(any()) } returns JustSuccess
            }
            val testUserData = UserData.fromUserAuthData(
                emailAuthData(
                    email = email,
                    metadata = metadata
                )
            )

            loginWithPublishableKeyUseCase(
                userRepository = mockk {
                    every { userData } returns mockUserDataSetting
                }
            ).execute(
                hypertrackSdk = mockk {
                    every { deviceID } returns TestInjector.TEST_DEVICE_ID.value
                    every { setDeviceName(capture(deviceNameSlot)) } returns mockk()
                    every { setDeviceMetadata(capture(deviceMetadataSlot)) } returns mockk()
                },
                userData = testUserData,
                publishableKey = TestInjector.TEST_PUBLISHABLE_KEY,
                deeplinkWithoutGetParams = null
            ).collect { result ->
                println(result)
                (result as Success).data.let {
                    assertEquals(TestInjector.TEST_PUBLISHABLE_KEY, it.publishableKey)
                    verify { mockUserDataSetting.save(testUserData) }
                    assertEquals(email.value.capitalize(), deviceNameSlot.captured)
                    assertEquals(
                        mapOf<String, Any>(
                            "email" to email.value
                        ) + metadata, deviceMetadataSlot.captured
                    )
                }
            }
        }
    }

    companion object {
        fun loginWithPublishableKeyUseCase(
            userRepository: UserRepository
        ): LoginWithPublishableKeyUseCase {
            return LoginWithPublishableKeyUseCase(
                refreshUserAccessTokenUseCase = mockk {
                    every { execute(any(), any()) } returns flowOf(
                        UserAccessToken("token").asSuccess()
                    )
                },
                accessTokenRepository = accessTokenRepository(),
                publishableKeyRepository = mockk {
                    every { publishableKey } returns mockk {
                        every { load() } returns TestInjector.TEST_PUBLISHABLE_KEY.asSuccess()
                        every { save(any()) } returns JustSuccess
                    }
                },
                userRepository = userRepository
            )
        }
    }

}
