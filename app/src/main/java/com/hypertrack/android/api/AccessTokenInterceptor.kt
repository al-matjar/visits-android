package com.hypertrack.android.api

import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.AUTH_HEADER_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.formatTokenHeader
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds access token to requests
 */
class AccessTokenInterceptor(private val accessTokenRepository: AccessTokenRepository) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessTokenRepository.accessToken
        val request = chain
            .request()
            .newBuilder().apply {
                token?.let {
                    addHeader(AUTH_HEADER_KEY, formatTokenHeader(token))
                }
            }
            .build()
        return chain.proceed(request)
    }

}
