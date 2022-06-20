package com.hypertrack.android.repository.preferences

import android.content.SharedPreferences
import com.hypertrack.android.utils.KeyValueEntry
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.android.utils.tryAsSimpleResult

open class SharedPreferencesNullableBooleanEntry(
    private val key: String,
    private val preferences: SharedPreferences,
) : KeyValueEntry<Boolean> {

    override fun save(data: Boolean?): SimpleResult {
        return tryAsSimpleResult {
            if (data != null) {
                preferences.edit().putBoolean(key, data).commit()
            } else {
                preferences.edit().remove(key).apply()
            }
        }
    }

    override fun load(): Result<Boolean?> {
        return tryAsResult {
            if (!preferences.contains(key)) {
                null
            } else {
                preferences.getBoolean(key, false)
            }
        }
    }
}
