package com.hypertrack.android.api

import android.os.Build
import com.hypertrack.logistics.android.github.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.userAgent

/**
 * Adds VisitsApp user agent to requests
 */
class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val networkingLibrary = userAgent
        val request = chain.request().newBuilder()
            .addHeader(
                "User-Agent",
                "VisitsApp/${BuildConfig.VERSION_NAME} $networkingLibrary Android/${Build.VERSION.RELEASE}"
            )
            .build()
        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            val url = request.url.toString()
            throw e
        }
    }

}
