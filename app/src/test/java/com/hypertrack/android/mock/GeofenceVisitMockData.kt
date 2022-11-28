package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Arrival
import com.hypertrack.android.api.Exit
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.api.Point
import com.hypertrack.android.api.RouteTo
import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.models.GraphQlDayVisitsStats
import com.hypertrack.android.api.graphql.models.GraphQlGeofence
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.api.graphql.models.GraphQlPlaceVisits
import com.hypertrack.android.api.graphql.models.GraphQlPointGeometry
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.datetime.toIso
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

// don't delete unusable code here, it can be used in main sources to mock responses
object GeofenceVisitMockData {

    fun createGeofenceVisit(
        _date: LocalDate? = null,
        deviceId: DeviceId = DeviceId("device_id"),
        arrival: ZonedDateTime? = null
    ): GeofenceVisit {
        val mockExitDt = arrival ?: _date?.let {
            ZonedDateTime.now().withMonth(it.monthValue).withDayOfMonth(it.dayOfMonth)
                .withHour((Math.random() * 7 + 7).toInt())
                .minusHours(1)
        } ?: ZonedDateTime.now()

        val dur = (Math.random() * 3600 + 3600).toInt()
        val mockArrivalDt = arrival ?: mockExitDt.minusSeconds(dur.toLong())

        val date = _date ?: LocalDate.now()
        return GeofenceVisit(
            "1",
            UUID.randomUUID().toString(),
            deviceId.value,
            Arrival(mockArrivalDt.toIso()),
            Exit(mockExitDt.toIso()),
            Point(1.0, 1.0),
            RouteTo(
                (Math.random() * 500).toInt() + 500,
                (Math.random() * 500).toInt() + 500,
                (Math.random() * 500).toInt() + 500,
            ),
            dur,
            null,
            null,
        )
    }

    fun createGraphQlGeofenceVisit(
        arrival: ZonedDateTime,
        position: LatLng = MockData.PALO_ALTO_LAT_LNG
    ): GraphQlGeofenceVisit {
        return GraphQlGeofenceVisit(
            "1",
            Arrival(arrival.toIso()),
            Exit(arrival.plusHours(1).toIso()),
            RouteTo(
                100,
                100,
                100,
            ),
            GraphQlGeofence(
                "1",
                "1",

                MockData.moshi.createAnyMapAdapter().toJson(
                    mapOf(
//                        "integration" to Integration(
//                            "id",
//                            "integration name"
//                        )
                    )
                ),
                GraphQlPointGeometry(
                    center = listOf(
                        position.longitude, position.latitude
                    ), null
                ),
                100
            ),
            null
        )
    }

    fun createGraphQlPlaceVisits(): GraphQlPlaceVisits {
        return mapOf(
            DayRange(
                LocalDate.now(),
                ZoneId.systemDefault()
            ).getQueryKey() to GraphQlDayVisitsStats(
                listOf(
                    createGraphQlGeofenceVisit(ZonedDateTime.now()),
                    createGraphQlGeofenceVisit(ZonedDateTime.now().minusHours(1)),
                ),
                1
            ),
            DayRange(
                LocalDate.now().minusDays(1),
                ZoneId.systemDefault()
            ).getQueryKey() to GraphQlDayVisitsStats(
                listOf(
                    createGraphQlGeofenceVisit(ZonedDateTime.now()),
                    createGraphQlGeofenceVisit(ZonedDateTime.now().minusHours(1)),
                ),
                1
            )
        )
    }

}
