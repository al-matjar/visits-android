package com.hypertrack.android.models.local

import com.hypertrack.android.utils.MyApplication
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Phone(val value: String) {
    override fun toString(): String = javaClass.simpleName
}
