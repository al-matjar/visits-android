package com.hypertrack.android.api.graphql.queries

import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.utils.DeviceId
import com.hypertrack.android.utils.PublishableKey
import com.hypertrack.android.utils.datetime.toIso

class PlacesVisitsQuery(
    deviceId: DeviceId,
    publishableKey: PublishableKey,
    days: List<DayRange>
) : GraphQlQuery {

    override val queryString: String = createQuery(deviceId, publishableKey, days)
    override val variables = mapOf<String, Any>()

    private fun createQuery(
        deviceId: DeviceId,
        publishableKey: PublishableKey,
        days: List<DayRange>
    ): String {
        val daysString = StringBuilder()
        days.forEach {
            daysString.append(
                """
                ${it.getQueryKey()}: getDeviceHistory(
                    device_id: "${deviceId.value}",
                    publishable_key: "${publishableKey.value}",
                    from_datetime:"${it.zonedDateTimeStart.toIso()}",
                    to_datetime:"${it.zonedDateTimeEnd.toIso()}"
                ) {
                    ...visitHistory
                }
            """.trimIndent() + "\n"
            )
        }
        return """
            query VisitHistory {
                $daysString
            }

            fragment visitHistory on DeviceHistory {
                drive_distance 
                geofence_markers {
                    ${GraphQlGeofenceVisit.FIELDS_QUERY}
                }
            }
        """.trimIndent()
    }
}
