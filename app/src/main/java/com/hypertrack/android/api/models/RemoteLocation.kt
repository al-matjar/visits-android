package com.hypertrack.android.api.models

import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime


@JsonClass(generateAdapter = true)
data class RemoteLocation(
    @field:Json(name = "coordinate") val coordinate: Coordinate,
    @field:Json(name = "recorded_at") val _recordedAt: String,
) {
    fun toLatLng() = coordinate.toLatLng()

    val recordedAt: ZonedDateTime
        get() = dateTimeFromString(_recordedAt)

    companion object {
        const val FIELDS_QUERY = """
            coordinate {
                latitude
                longitude
            }
            recorded_at
        """
    }
}
