package com.hypertrack.android.api

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.AUTH_HEADER_KEY
import com.hypertrack.android.repository.access_token.AccessTokenRepository.Companion.formatTokenHeader
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.withOverwrittenHeader
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Refreshes access token on auth error
 */
class AccessTokenAuthenticator(
    private val deviceId: DeviceId,
    private val publishableKey: PublishableKey,
    private val accessTokenRepository: AccessTokenRepository,
    private val crashReportsProvider: CrashReportsProvider
) : Authenticator {

    // called if client gets auth error
    override fun authenticate(route: Route?, response: Response): Request? {
        return try {
            synchronized(this) {
                crashReportsProvider.log(
                    "Refreshing token for ${response.request.url.encodedPath}"
                )
                /**
                 * because this method is called when there is 401 error
                 * this means that current token is invalid
                 */
                val oldAccessToken = accessTokenRepository.accessToken

                accessTokenRepository.refreshToken(
                    publishableKey, deviceId
                ).let { accessTokenResult ->
                    when (accessTokenResult) {
                        is Success -> {
                            /**
                             * if the new token on the backend is fresh,
                             * it returns the same token for each request,
                             * so if the returned token is the same as old one
                             * we need to stop refreshing to avoid infinite loop
                             */
                            if (accessTokenResult.data == oldAccessToken) {
                                null
                            } else {
                                accessTokenRepository.accessToken = accessTokenResult.data
                                response.request.withOverwrittenHeader(
                                    AUTH_HEADER_KEY,
                                    formatTokenHeader(accessTokenResult.data)
                                )
                            }
                        }
                        is Failure -> {
                            crashReportsProvider.logException(accessTokenResult.exception)
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
            null
        }
    }

}
