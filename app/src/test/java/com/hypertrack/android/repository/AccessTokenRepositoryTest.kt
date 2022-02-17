package com.hypertrack.android.repository

import com.hypertrack.android.TestInjector
import com.hypertrack.android.models.auth.AuthCallResponse
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.access_token.AccessTokenRepository
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
import junit.framework.TestCase.assertTrue
import okhttp3.Request
import org.junit.Test

class AccessTokenRepositoryTest {

    @Test
    fun `it should request new access token`() {
        val publishableKey = RealPublishableKey("publishable_key")
        val deviceId = DeviceId("device_id")

        val slot = slot<Request>()
        val token = "token"

        val repository = AccessTokenRepository(
            "http://google.com",
            moshi = TestInjector.getMoshi(),
            crashReportsProvider = mockk(),
            preferencesRepository = mockk(),
            okHttpClient = mockk() {
                every { newCall(capture(slot)) } returns mockk {
                    every { execute() } returns mockk {
                        every { isSuccessful } returns true
                        every { body } returns mockk {
                            every { string() } returns TestInjector.getMoshi()
                                .adapter(AuthCallResponse::class.java)
                                .toJson(
                                    AuthCallResponse(
                                        accessToken = token,
                                        expiresIn = 11111
                                    )
                                )
                        }
                        every { close() } returns Unit
                    }
                }
            }
        )

        repository.refreshToken(publishableKey, deviceId).let { result ->
            (result as Success).let {
                assertEquals(token, it.data.value)
            }
        }
    }

    @Test
    fun `it should get access token from preferences`() {
        val tokenResult = UserAccessToken("token")
        val repository = AccessTokenRepository(
            "url",
            moshi = mockk(),
            crashReportsProvider = mockk(),
            preferencesRepository = mockk {
                every { accessToken } returns mockk {
                    every { load() } returns tokenResult.asSuccess()
                }
            },
            okHttpClient = mockk()
        )

        repository.accessToken.let {
            assertEquals(tokenResult, it)
        }
    }

    @Test
    fun `it should save access token to preferences`() {
        val tokenResult = UserAccessToken("token")
        val tokenEntry = mockk<SharedPreferencesStringEntry<UserAccessToken>> {
            every { save(any()) } returns JustSuccess
        }
        val repository = AccessTokenRepository(
            "url",
            moshi = mockk(),
            crashReportsProvider = mockk(),
            preferencesRepository = mockk {
                every { accessToken } returns tokenEntry
            },
            okHttpClient = mockk()
        )

        repository.accessToken = tokenResult
        verify { tokenEntry.save(tokenResult) }
    }

}
