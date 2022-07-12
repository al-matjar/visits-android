package com.hypertrack.android.api

import com.hypertrack.android.TestInjector
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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
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

        authenticator.authenticate(
            route = mockk(),
            response = response(request(validToken))
        )!!.let { request ->
            assertEquals(1, request.headers(AUTH_HEADER_KEY).size)
            assertEquals(formatTokenHeader(validToken), request.headers(AUTH_HEADER_KEY).first())
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
            response = response(request(oldToken))
        ).let {
            assertNull(it)
        }
    }

    companion object {
        fun request(
            token: UserAccessToken?
        ): Request {
            return Request.Builder()
                .url(TestInjector.TEST_URL)
                .apply {
                    token?.let { addHeader(AUTH_HEADER_KEY, formatTokenHeader(token)) }
                }
                .build()
        }

        fun response(
            request: Request
        ): Response {
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("")
                .code(401)
                .body(
                    ResponseBody.create(
                        "application/json".toMediaTypeOrNull(),
                        "{}"
                    )
                )
                .build()
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
