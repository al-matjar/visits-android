package com.hypertrack.android.repository.preferences

import com.hypertrack.android.interactors.app.Email
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.models.auth.BasicAuthAccessTokenConfig
import com.hypertrack.android.repository.MyPreferences
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.tryAsResult
import com.squareup.moshi.Moshi
import org.json.JSONObject

class PreferencesRepository(
    private val myPreferences: MyPreferences,
    private val moshi: Moshi,
) {

    private val preferences = myPreferences.sharedPreferences

    val publishableKey = object : SharedPreferencesStringEntry<RealPublishableKey>(
        KEY_PUBLISHABLE_KEY,
        preferences
    ) {
        override fun load(): Result<RealPublishableKey?> {
            return super.load().flatMap {
                it?.let { Success(it) }
                    ?: tryAsResult {
                        getLegacyPublishableKey().also { legacyPublishableKey ->
                            save(legacyPublishableKey)
                        }
                    }
            }
        }

        override fun serialize(data: RealPublishableKey): Result<String> {
            return data.value.asSuccess()
        }

        override fun deserialize(rawData: String): Result<RealPublishableKey> {
            return RealPublishableKey(rawData).asSuccess()
        }
    }

    val accessToken = object : SharedPreferencesStringEntry<UserAccessToken>(
        KEY_ACCESS_TOKEN,
        preferences
    ) {
        override fun load(): Result<UserAccessToken?> {
            return super.load().flatMap {
                it?.let { Success(it) }
                    ?: tryAsResult {
                        getLegacyAccessToken().also { legacyAccessToken ->
                            save(legacyAccessToken)
                        }
                    }
            }
        }

        override fun serialize(data: UserAccessToken): Result<String> {
            return data.value.asSuccess()
        }

        override fun deserialize(rawData: String): Result<UserAccessToken> {
            return UserAccessToken(rawData).asSuccess()
        }
    }

    val userData = object : SharedPreferencesStringEntry<UserData>(
        KEY_USER_DATA,
        preferences
    ) {
        override fun load(): Result<UserData?> {
            return super.load().flatMap {
                it?.let { Success(it) }
                    ?: tryAsResult {
                        getLegacyUserData().also { legacyUserData ->
                            save(legacyUserData)
                        }
                    }
            }
        }

        override fun serialize(data: UserData): Result<String> {
            return tryAsResult {
                moshi.adapter(UserData::class.java).toJson(data)
            }
        }

        override fun deserialize(rawData: String): Result<UserData> {
            return tryAsResult {
                moshi.adapter(UserData::class.java).fromJson(rawData)!!
            }
        }
    }

    var trackingStartedOnFirstLaunch = SharedPreferencesNullableBooleanEntry(
        KEY_FIRST_LAUNCH,
        preferences,
    )

    var measurementUnitsImperial = SharedPreferencesNullableBooleanEntry(
        KEY_UNITS_IMPERIAL,
        preferences
    )

    private fun getLegacyAccessToken(): UserAccessToken? {
        return try {
            preferences.getString(MyPreferences.ACCESS_REPO_KEY, null)?.let {
                moshi.adapter(BasicAuthAccessTokenConfig::class.java).fromJson(it)?.token
            }?.let {
                UserAccessToken(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLegacyPublishableKey(): RealPublishableKey? {
        return myPreferences.getAccountData().publishableKey?.let {
            RealPublishableKey(it)
        }
    }

    private fun getLegacyUserData(): UserData? {
        return preferences.getString(LEGACY_USER_KEY, null)?.let {
            UserData(
                email = Email(JSONObject(it).getString(LEGACY_KEY_EMAIL)),
                phone = null,
                metadata = null
            )
        }
    }


    companion object {
        private const val LEGACY_KEY_EMAIL = "email"
        private const val LEGACY_USER_KEY = "com.hypertrack.android.utils.user"
        private const val PREFIX = "com.hypertrack.android."
        private const val KEY_UNITS_IMPERIAL = PREFIX + "units_imperial"
        private const val KEY_ACCESS_TOKEN = PREFIX + "access_token"
        private const val KEY_USER_DATA = PREFIX + "user_data"
        private const val KEY_PUBLISHABLE_KEY = PREFIX + "publishable_key"
        private const val KEY_FIRST_LAUNCH = PREFIX + "first_launch"
    }

}
