package com.hypertrack.android.repository.preferences

import android.content.SharedPreferences
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import okhttp3.internal.cache2.Relay.Companion.edit
import org.junit.Test

class SharedPreferencesStringEntryTest {

    @Test
    fun `it should save valid value`() {
        val editor = editor()
        val entry = entry(sharedPreferences(editor))
        entry.save(1)
        verify { editor.putString(KEY, "1") }
        verify { editor.apply() }
    }

    @Test
    fun `it should save null value`() {
        val editor = editor()
        val entry = entry(sharedPreferences(editor))
        entry.save(null)
        verify { editor.remove(KEY) }
        verify { editor.apply() }
    }

    @Test
    fun `it should load valid value`() {
        val prefs = sharedPreferences(editor(), value = 1)
        val entry = entry(prefs)
        entry.load().let {
            assertEquals(1.asSuccess(), it)
            verify { prefs.getString(KEY, null) }
        }
    }

    @Test
    fun `it should load null value`() {
        val prefs = sharedPreferences(editor(), value = null)
        val entry = entry(prefs)
        entry.load().let {
            assertEquals(Success<Int?>(null), it)
            verify { prefs.getString(KEY, null) }
        }
    }

    companion object {
        private const val KEY = "key"

        fun entry(prefs: SharedPreferences): SharedPreferencesStringEntry<Int> {
            return object : SharedPreferencesStringEntry<Int>(KEY, prefs) {
                override fun serialize(data: Int): Result<String> {
                    return data.toString().asSuccess()
                }

                override fun deserialize(rawData: String): Result<Int> {
                    return rawData.toInt().asSuccess()
                }

            }
        }

        fun sharedPreferences(
            editor: SharedPreferences.Editor,
            value: Int? = null
        ): SharedPreferences {
            return mockk {
                every { edit() } returns editor
                every { getString(KEY, null) } returns value?.toString()
            }
        }

        fun editor(): SharedPreferences.Editor {
            return mockk {
                every { putString(KEY, any()) } returns this
                every { remove(KEY) } returns this
                every { apply() } returns Unit
            }
        }
    }

}
