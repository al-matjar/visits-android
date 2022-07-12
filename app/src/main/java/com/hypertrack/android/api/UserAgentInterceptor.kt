package com.hypertrack.android.api

import android.os.Build
import com.hypertrack.android.utils.withOverwrittenHeader
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
        val request = chain.request().withOverwrittenHeader(
            "User-Agent",
            "VisitsApp/${BuildConfig.VERSION_NAME} $networkingLibrary Android/${Build.VERSION.RELEASE}"
        )
        return chain.proceed(request)
    }

}
