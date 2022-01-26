package com.hypertrack.android.api.graphql.models

import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class GraphQlDeviceStatusMarker(
    // null if it is ongoing marker
    @field:Json(name = "id") val id: String?,
    //todo check nullability
    @field:Json(name = "start_datetime") val startDatetimeString: String,
    @field:Json(name = "end_datetime") val endDatetimeString: String?,
    @field:Json(name = "type") val data: GraphQlDeviceStatusMarkerData,
) {
    val startDatetime: ZonedDateTime = dateTimeFromString(startDatetimeString)
    val endDatetime: ZonedDateTime? = endDatetimeString?.let(::dateTimeFromString)

    companion object {
        const val FIELDS_QUERY = """
            end_datetime
            id
            start_datetime
            type {
                ... on DeviceStatusMarkerActive {
                    __typename
                    ${GraphQlDeviceStatusMarkerActiveData.FIELDS_QUERY}
                }
                ... on DeviceStatusMarkerInactive {
                    __typename
                    ${GraphQlDeviceStatusMarkerInactiveData.FIELDS_QUERY}
                }
            }
        """
    }
}

sealed class GraphQlDeviceStatusMarkerData

@JsonClass(generateAdapter = true)
class GraphQlDeviceStatusMarkerActiveData(
    @field:Json(name = "activity") val activity: String?,
    @field:Json(name = "distance") val distance: Int?,
    @field:Json(name = "steps") val steps: Int?,
    @field:Json(name = "start_address") val startAddress: GraphQlAddress?,
    @field:Json(name = "end_address") val endAddress: GraphQlAddress?,
    @field:Json(name = "start_location") val startLocation: RemoteLocation?,
    @field:Json(name = "end_location") val endLocation: RemoteLocation?,
) : GraphQlDeviceStatusMarkerData() {
    companion object {
        const val FIELDS_QUERY = """
            activity
            distance
            steps
            end_address {
                address
                place
            }
            end_location {
                coordinate {
                    latitude
                    longitude
                }
                recorded_at
            }
            start_address {
                address
                place
            }
            start_location {
                coordinate {
                    latitude
                    longitude
                }
                recorded_at
            }
        """
    }
}

@JsonClass(generateAdapter = true)
class GraphQlDeviceStatusMarkerInactiveData(
    @field:Json(name = "reason") val reason: String
) : GraphQlDeviceStatusMarkerData() {
    companion object {
        const val FIELDS_QUERY = """
            reason
        """
    }
}
