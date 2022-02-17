package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RefreshUserAccessTokenUseCaseTest {

    @Test
    fun `it should refresh user access token`() {
        val publishableKey = RealPublishableKey("publishable_key")
        val deviceId = DeviceId("device_id")
        val token = UserAccessToken("token")

        runBlocking {
            RefreshUserAccessTokenUseCase(accessTokenRepository = mockk {
                coEvery { refreshToken(eq(publishableKey), eq(deviceId)) } returns token.asSuccess()
            }).execute(publishableKey, deviceId).collect { result ->
                (result as Success).let {
                    assertEquals(token, it.data)
                }
            }
        }
    }
}
