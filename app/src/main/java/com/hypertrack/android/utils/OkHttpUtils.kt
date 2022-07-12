package com.hypertrack.android.utils

import okhttp3.Request

fun Request.withOverwrittenHeader(key: String, value: String?): Request {
    return this.newBuilder()
        .removeHeader(key)
        .apply {
            if (value != null) {
                addHeader(key, value)
            }
        }
        .build()
}
