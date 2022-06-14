package com.hypertrack.android.repository.access_token

import com.hypertrack.android.models.auth.AuthCallResponse
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.exception.SimpleException
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toNullableWithErrorReporting
import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.HttpURLConnection

class AccessTokenRepository(
    private val authUrl: String,
    private val moshi: Moshi,
    private val preferencesRepository: PreferencesRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val okHttpClient: OkHttpClient
) {

    /**
     * this method should be thread safe
     * shared preferences impl are thread safe out of the box
     */
    var accessToken: UserAccessToken?
        get() {
            return preferencesRepository.accessToken.load()
                .toNullableWithErrorReporting(crashReportsProvider)
        }
        set(value) {
            preferencesRepository.accessToken.save(value)
        }

    /**
     * in the context of logging in publishable key is like the refresh token
     * in refresh/access token pattern
     */
    fun refreshToken(publishableKey: PublishableKey, deviceId: DeviceId): Result<UserAccessToken> {
        return try {
            okHttpClient
                .newCall(createRequest(deviceId, publishableKey.value))
                .execute()
                .use { response -> getTokenFromResponse(response) }
                .let { result ->
                    when (result) {
                        is Active -> {
                            Success(UserAccessToken(result.token))
                        }
                        is Error -> Failure(result.exception)
                        InvalidCredentials -> Failure(SimpleException(result.toString()))
                        Suspended -> Failure(SimpleException(result.toString()))
                    }
                }
        } catch (e: Exception) {
            Failure(e)
        }
    }

    private fun getTokenFromResponse(response: Response): TokenResult {
        return when {
            response.isSuccessful -> {
                response.body?.let {
                    try {
                        val responseObject = moshi.adapter(AuthCallResponse::class.java)
                            .fromJson(it.string())
                            ?: return@let Error(IllegalArgumentException("failed to parse token JSON"))
                        Active(responseObject.accessToken)
                    } catch (e: Exception) {
                        Error(e)
                    }
                } ?: Error(IllegalArgumentException("token body == null"))
            }
            response.code == HttpURLConnection.HTTP_FORBIDDEN && response.body?.string()
                ?.contains("trial ended") == true -> {
                Suspended
            }
            response.code == HttpURLConnection.HTTP_UNAUTHORIZED -> {
                InvalidCredentials
            }
            else -> {
                Error(Exception("Failed to get token: ${response.code} $response"))
            }
        }
    }


    private fun createRequest(
        deviceId: DeviceId,
        publishableKey: String,
    ): Request {
        return Request.Builder()
            .url(authUrl)
            .header(AUTH_HEADER_KEY, Credentials.basic(publishableKey, ""))
            .post("""{"device_id": "${deviceId.value}"}""".toRequestBody(MEDIA_TYPE_JSON))
            .build()
    }

    companion object {
        val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
        const val AUTH_HEADER_KEY = "Authorization"
        private const val BEARER = "Bearer"

        fun formatTokenHeader(token: UserAccessToken): String {
            return "$BEARER ${token.value}"
        }
    }

}


