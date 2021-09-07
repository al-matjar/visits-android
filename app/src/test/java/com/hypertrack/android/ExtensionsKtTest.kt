package com.hypertrack.android

import com.hypertrack.android.utils.toNote
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsKtTest {

    @Test
    fun `it should concatenate all the keys and values into one string`() {
        val payload = mapOf<String, Any>(
                "myKey" to "myValue",
                "anotherKey" to "anotherValue"
        )
        val got = payload.toNote()
        assertEquals("myKey: myValue\nanotherKey: anotherValue", got)
    }

    @Test
    fun `it should exclude entries from note if the key starts with ht_`() {
        val payload = mapOf<String, Any>(
                "myKey" to "myValue",
                "anotherKey" to "anotherValue",
                "ht_invisible_key" to "invisibleValue"
        )
        val got = payload.toNote()
        assertEquals("myKey: myValue\nanotherKey: anotherValue", got)
    }
}