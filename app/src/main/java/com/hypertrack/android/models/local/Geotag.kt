package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.hypertrack.android.api.graphql.models.GraphQlGeotagMarker
import com.hypertrack.android.utils.JsonMap
import com.hypertrack.android.utils.JsonObjectString
import com.hypertrack.android.utils.parse
import com.squareup.moshi.Moshi
import java.time.ZonedDateTime

data class Geotag(
    val id: String,
    val createdAt: ZonedDateTime,
    val location: LatLng,
    val metadata: JsonMap,
    val address: String?
) {
    companion object {
        fun fromGraphQlGeotagMarker(marker: GraphQlGeotagMarker, moshi: Moshi): Geotag {
            return Geotag(
                id = marker.id,
                location = marker.location,
                createdAt = marker.createdAt,
                metadata = moshi.parse(marker.metadata),
                address = null
            )
        }
    }
}
