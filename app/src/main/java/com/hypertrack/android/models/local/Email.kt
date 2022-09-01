package com.hypertrack.android.models.local

import com.hypertrack.android.utils.MyApplication
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Email(val value: String) {
    override fun toString(): String {
        return if (MyApplication.DEBUG_MODE) {
            super.toString()
        } else {
            javaClass.simpleName
        }
    }
}
