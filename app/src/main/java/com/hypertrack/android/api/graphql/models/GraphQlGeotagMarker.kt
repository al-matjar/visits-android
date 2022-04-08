package com.hypertrack.android.api.graphql.models

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
class GraphQlGeotagMarker(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "recorded_at") val recordedAtString: String,
    @field:Json(name = "location") val remoteLocation: RemoteLocation,
    @field:Json(name = "metadata") val metadata: String,
) {
    val createdAt: ZonedDateTime = dateTimeFromString(recordedAtString)

    val location = remoteLocation.coordinate.toLatLng()

    companion object {
        const val FIELDS_QUERY = """
         id
         location {
             coordinate {
                latitude
                longitude
             }
             recorded_at
         }
         metadata
         recorded_at
         route_to {
             distance
             duration
         }
     """
    }
}
