package com.squareup.moshi.recipes

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeJsonAdapter {
    @ToJson
    fun dtToJson(dt: ZonedDateTime): Map<String, Any> {
        return mapOf("iso_timestamp" to dt.format(DateTimeFormatter.ISO_DATE_TIME))
    }

    @FromJson
    fun jsonToDt(json: Map<String, Any>): ZonedDateTime {
        return ZonedDateTime.parse(json["iso_timestamp"] as String)
    }
}
