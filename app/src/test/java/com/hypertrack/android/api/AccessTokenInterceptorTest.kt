package com.hypertrack.android.api

import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.accessTokenRepository
import com.hypertrack.android.api.AccessTokenAuthenticatorTest.Companion.builderMocks
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.AUTH_HEADER_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.formatTokenHeader
import com.hypertrack.android.repository.access_token.UserAccessToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

class AccessTokenInterceptorTest {

    @Test
    fun `it should add token header to request`() {
        val token = UserAccessToken("token_to_append")
        val interceptor = AccessTokenInterceptor(
            accessTokenRepository = accessTokenRepository(
                oldToken = token
            )
        )

        var builder: Request.Builder? = null
        val request = AccessTokenAuthenticatorTest.request {
            builder = builderMocks(it)
        }
        val response = mockk<Response>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(request) } returns response
        }
        interceptor.intercept(
            chain = chain
        ).let {
            assertEquals(response, it)
        }

        verify { builder!!.addHeader(AUTH_HEADER_KEY, formatTokenHeader(token)) }
        verify { builder!!.build() }
    }

}
