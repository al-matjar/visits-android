package com.hypertrack.android.repository

import com.hypertrack.android.models.local.RealPublishableKey

class PreferencesRepository(
    private val myPreferences: MyPreferences
) {

    private val preferences = myPreferences.sharedPreferences

    val publishableKey: RealPublishableKey?
        get() = myPreferences.getAccountData().publishableKey?.let {
            RealPublishableKey(it)
        }

    var measurementUnitsImperial: Boolean?
        get() {
            return if (!preferences.contains(KEY_UNITS_IMPERIAL)) {
                return null
            } else {
                preferences.getBoolean(KEY_UNITS_IMPERIAL, false)
            }
        }
        set(value) {
            if (value != null) {
                preferences.edit().putBoolean(KEY_UNITS_IMPERIAL, value).apply()
            } else {
                preferences.edit().remove(KEY_UNITS_IMPERIAL).apply()
            }
        }

    companion object {
        private const val PREFIX = "com.hypertrack.android"
        private const val KEY_UNITS_IMPERIAL = PREFIX + "units_imperial"
    }

}
