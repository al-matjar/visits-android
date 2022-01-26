package com.hypertrack.android.api.graphql.queries

import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarker
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.api.graphql.models.GraphQlGeotagMarker
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.utils.DeviceId
import com.hypertrack.android.utils.PublishableKey
import com.hypertrack.android.utils.datetime.toIso

import com.squareup.moshi.JsonClass

class HistoryQuery(
    deviceId: DeviceId,
    publishableKey: PublishableKey,
    day: DayRange
) : GraphQlQuery {

    override val queryString: String = createQuery(deviceId, publishableKey, day)
    override val variables = mapOf<String, Any>()

    private fun createQuery(
        deviceId: DeviceId,
        publishableKey: PublishableKey,
        day: DayRange
    ): String {
        return """
            query History {
                result: getDeviceHistory(
                    device_id: "${deviceId.value}",
                    publishable_key: "${publishableKey.value}",
                    from_datetime:"${day.zonedDateTimeStart.toIso()}",
                    to_datetime:"${day.zonedDateTimeEnd.toIso()}"
                ) {
                    geofence_markers {
                        ${GraphQlGeofenceVisit.FIELDS_QUERY}
                    }
                    geotag_markers {
                        ${GraphQlGeotagMarker.FIELDS_QUERY}
                    }
                    locations {
                        ${RemoteLocation.FIELDS_QUERY}
                    }
                    drive_distance
                    drive_duration
                    device_status_markers {
                        ${GraphQlDeviceStatusMarker.FIELDS_QUERY}
                    }
                }
            }
        """.trimIndent()
    }
}

/*


*/
