package com.hypertrack.android.api

import com.hypertrack.android.TestInjector.TEST_DEVICE_ID
import com.hypertrack.android.TestInjector.TEST_PUBLISHABLE_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.AUTH_HEADER_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.formatTokenHeader
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import com.hypertrack.android.utils.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

class AccessTokenAuthenticatorTest {

    @Test
    fun `it should refresh access token on auth error`() {
        val validToken = UserAccessToken("token")
        val oldToken = UserAccessToken("old")
        val accessTokenRepository = accessTokenRepository(
            oldToken = oldToken,
            newToken = validToken
        )
        val authenticator = authenticator(
            accessTokenRepository = accessTokenRepository
        )

        var testRequest: Request? = null
        var builder: Request.Builder? = null

        authenticator.authenticate(
            route = mockk(),
            response = response(oldToken) { request ->
                testRequest = request
                builder = builderMocks(request)
            }
        )!!.let { request ->
            verify { request.newBuilder() }
            verify {
                builder!!.addHeader(
                    AUTH_HEADER_KEY,
                    formatTokenHeader(validToken)
                )
            }
            verify { builder!!.build() }
            verify { accessTokenRepository.accessToken = validToken }
            assertEquals(testRequest, request)
        }
    }

    @Test
    fun `it should return null on auth error if access token was refreshed`() {
        val oldToken = UserAccessToken("old")

        authenticator(
            accessTokenRepository = accessTokenRepository(
                oldToken = oldToken,
                newToken = oldToken
            )
        ).authenticate(
            route = mockk(),
            response = response(
                oldToken
            )
        ).let {
            assertNull(it)
        }
    }

    companion object {
        fun builderMocks(request: Request): Request.Builder {
            val builder = mockk<Request.Builder> {
                every { addHeader(any(), any()) } returns this
                every { build() } returns request
            }
            every { request.newBuilder() } returns builder
            return builder
        }

        fun request(
            additionalMocks: (Request) -> Unit = {}
        ): Request {
            return mockk() {
                every { url } returns mockk() {
                    every { encodedPath } returns "path"
                }
                additionalMocks.invoke(this)
            }
        }

        fun response(
            token: UserAccessToken,
            additionalMocks: (Request) -> Unit = {}
        ): Response {
            return mockk() {
                every { request } answers { request(additionalMocks) }
                every { header(any(), any()) } returns formatTokenHeader(token)
            }
        }

        fun authenticator(
            accessTokenRepository: AccessTokenRepository,
        ): AccessTokenAuthenticator {
            return AccessTokenAuthenticator(
                TEST_DEVICE_ID,
                TEST_PUBLISHABLE_KEY,
                accessTokenRepository = accessTokenRepository,
                crashReportsProvider = crashReportsProvider(),
            )
        }

        fun accessTokenRepository(
            oldToken: UserAccessToken = UserAccessToken("default_token"),
            newToken: UserAccessToken = UserAccessToken("default_token")
        ): AccessTokenRepository {
            return mockk() {
                every { accessToken } returns oldToken
                every { accessToken = any() } returns Unit
                every { refreshToken(any(), any()) } returns Success(newToken)
            }
        }
    }

}
