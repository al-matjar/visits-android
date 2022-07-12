package com.hypertrack.android.api

import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.accessTokenRepository
import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.request
import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.response
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.AUTH_HEADER_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.formatTokenHeader
import com.hypertrack.android.repository.access_token.UserAccessToken
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import org.mockito.Mockito.mock

class AccessTokenInterceptorTest {

    @Test
    fun `it should add token header to request if token is not present`() {
        val token = UserAccessToken("token_to_append")
        val interceptor = AccessTokenInterceptor(
            accessTokenRepository = accessTokenRepository(
                oldToken = token
            )
        )

        val slot = slot<Request>()
        val request = request(token = null)
        interceptor.intercept(chain(request, slot)).let {
            slot.captured.let { modifiedRequest ->
                assertEquals(1, modifiedRequest.headers(AUTH_HEADER_KEY).size)
                assertEquals(
                    formatTokenHeader(token),
                    modifiedRequest.headers(AUTH_HEADER_KEY).first()
                )
            }
        }
    }

    @Test
    fun `it should add new token header to request`() {
        val oldToken = UserAccessToken("old_token")
        val token = UserAccessToken("token_to_append")
        val interceptor = AccessTokenInterceptor(
            accessTokenRepository = accessTokenRepository(
                oldToken = token
            )
        )

        val slot = slot<Request>()
        val request = request(oldToken)
        interceptor.intercept(chain(request, slot)).let {
            slot.captured.let { modifiedRequest ->
                assertEquals(1, modifiedRequest.headers(AUTH_HEADER_KEY).size)
                assertEquals(
                    formatTokenHeader(token),
                    modifiedRequest.headers(AUTH_HEADER_KEY).first()
                )
            }
        }
    }

    companion object {
        fun chain(request: Request, slot: CapturingSlot<Request>): Interceptor.Chain {
            return mockk {
                every { request() } returns request
                every { proceed(capture(slot)) } returns mockk()
            }
        }
    }

}
