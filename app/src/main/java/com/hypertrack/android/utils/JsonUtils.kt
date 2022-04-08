package com.hypertrack.android.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONObject

fun String.prettifyJson(): String {
    return try {
        JSONObject(this).toString(4)
    } catch (_: Exception) {
        this
    }
}

fun Moshi.mapToJson(map: Map<String, Any>): String {
    return this.createAnyMapAdapter().toJson(map)
}

fun Moshi.parse(jsonString: String): Map<String, Any> {
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
