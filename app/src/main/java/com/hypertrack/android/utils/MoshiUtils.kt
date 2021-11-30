package com.hypertrack.android.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

fun Moshi.createStringMapAdapter(): JsonAdapter<Map<String, String>> {
    return adapter(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )!!
}

fun Moshi.createAnyMapAdapter(): JsonAdapter<Map<String, Any>> {
    return adapter(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )!!
}