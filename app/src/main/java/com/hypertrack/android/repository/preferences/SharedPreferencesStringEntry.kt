package com.hypertrack.android.repository.preferences

import android.content.SharedPreferences
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.KeyValueEntry
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.android.utils.tryAsSimpleResult

abstract class SharedPreferencesStringEntry<T>(
    private val key: String,
    private val preferences: SharedPreferences
) : KeyValueEntry<T> {

    override fun save(data: T?): SimpleResult {
        return try {
            if (data != null) {
                serialize(data).flatMapSimple {
                    preferences.edit().putString(key, it).apply()
                    JustSuccess
                }
            } else {
                preferences.edit().remove(key).apply()
                JustSuccess
            }
        } catch (e: Exception) {
            JustFailure(e)
        }
    }

    override fun load(): Result<T?> {
        return try {
            preferences.getString(key, null).let { rawData: String? ->
                rawData?.let {
                    deserialize(it)
                        // to avoid casting issue
                        .map { item -> item }
                }
                    ?: Success(null)
            }
        } catch (e: Exception) {
            Failure(e)
        }
    }

    protected abstract fun serialize(data: T): Result<String>

    protected abstract fun deserialize(rawData: String): Result<T>
}
