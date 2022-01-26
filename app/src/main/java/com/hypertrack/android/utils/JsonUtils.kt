package com.hypertrack.android.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

typealias JsonObjectString = String
typealias JsonMap = Map<String, Any>

fun Moshi.mapToJson(map: JsonMap): JsonObjectString {
    return this.createAnyMapAdapter().toJson(map)
}

fun Moshi.parse(jsonString: JsonObjectString): Map<String, Any> {
    return this.createAnyMapAdapter().fromJson(jsonString)
        ?: throw IllegalArgumentException(jsonString)
}

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
