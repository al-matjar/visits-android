package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.models.GraphQlDayVisitsStats
import com.hypertrack.android.api.graphql.models.GraphQlGeofence
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.api.graphql.models.GraphQlGeotagMarker
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.api.graphql.models.GraphQlPlaceVisits
import com.hypertrack.android.api.graphql.models.GraphQlPointGeometry
import com.hypertrack.android.api.models.Coordinate
import com.hypertrack.android.api.models.RemoteGeofence
import com.hypertrack.android.api.models.RemoteEstimate
import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.ui.common.util.copy

import com.hypertrack.android.utils.datetime.toIso
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object TestMockData {

    private val paloAltoLatLng = LatLng(37.381451, -122.063616)
    val PALO_ALTO_LAT_LNG = paloAltoLatLng
    val DEVICE_ID_ALEX = DeviceId("9882686F-7A67-4B2F-B707-359EAF3B3376")

    private val addr = listOf(
        "2875 El Camino Real",
        "567 Melville Ave",
        "1295 Middlefield Rd",
        "630 Seale Ave",
        "1310 Bryant St",
        "475 Homer Ave",
        "1102 Ramona St",
        "117 University Ave",
        "130 Lytton Ave",
    )

    fun createGeofence(
        page: Int? = 0,
        lat: Double = 37.794763,
        lon: Double = 122.395223,
        polygon: Boolean = false,
        metadata: Map<String, Any> = mapOf("name" to page.toString()),
        marker: GeofenceMarkersResponse? = null,
    ): RemoteGeofence {
        val geometry = if (!polygon) {
            """
                {
                "type":"Point",
                "coordinates": [${lon ?: 37.794763}, ${lat ?: 122.395223}]
            }
            """.let {
                Injector.getMoshi().adapter(Geometry::class.java).fromJson(it)!!
            }
        } else {
            val coords: MutableList<List<Double>> = mutableListOf()
            coords.add(listOf(lon, lat))
            coords.add(listOf(lon + 0.001, lat))
            coords.add(listOf(lon + 0.001, lat + 0.001))
            coords.add(listOf(lon, lat + 0.001))
            Polygon(listOf(coords))
        }

        return RemoteGeofence(
            UUID.randomUUID().hashCode().toString(),
            "",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            metadata,
            geometry,
            marker,
            (1000 * Math.random()).toInt(),
            "",
            false,
        )
    }

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
        position: LatLng = PALO_ALTO_LAT_LNG
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
                "{}",
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

    fun createTrip(
        id: String? = null,
        orders: List<RemoteOrder> = listOf(
            createOrder(status = OrderStatus.ONGOING),/*.copy(estimate=null)*/
            createOrder(status = OrderStatus.ONGOING),
            createOrder(status = OrderStatus.SNOOZED),
            createOrder(status = OrderStatus.CANCELED),
            createOrder(status = OrderStatus.COMPLETED),
        )
    ): Trip {
        return Trip(
            id ?: "tripId " + Math.random(),
            null,
            TripStatus.ACTIVE.value,
            ZonedDateTime.now().toIso(),
            mapOf(),
            null,
            RemoteEstimate(
                ZonedDateTime.now().toIso(),
                null
            ),
            orders,
        )
    }

    fun createOrder(
        id: String? = null,
        status: OrderStatus = OrderStatus.ONGOING
    ): RemoteOrder {
        return RemoteOrder(
            id ?: ("order " + Math.random()),
            TripDestination(paloAltoLatLng, "Test Address"),
            status.value,
            ZonedDateTime.now().toIso(),
            ZonedDateTime.now().toIso(),
            RemoteEstimate(ZonedDateTime.now().plusMinutes(29).toIso(), null),
            mapOf(),
        )
    }

    fun createGraphQlHistory(): GraphQlHistory {
        return GraphQlHistory(
            listOf(
                createGraphQlGeofenceVisit(
                    ZonedDateTime.now(), LatLng(
                        PALO_ALTO_LAT_LNG.latitude + 0.01,
                        PALO_ALTO_LAT_LNG.longitude
                    )
                ),
                createGraphQlGeofenceVisit(
                    ZonedDateTime.now(), LatLng(
                        PALO_ALTO_LAT_LNG.latitude,
                        PALO_ALTO_LAT_LNG.longitude + 0.01
                    )
                ),
            ),
            listOf(),
            listOf(
                GraphQlGeotagMarker(
                    "1",
                    ZonedDateTime.now().toIso(),
                    createRemoteLocation(
                        PALO_ALTO_LAT_LNG.copy(longitude = PALO_ALTO_LAT_LNG.longitude - 0.01)
                    ),
                    "{}"
                )
            ),
            listOf(
                createRemoteLocation(
                    PALO_ALTO_LAT_LNG.copy(latitude = PALO_ALTO_LAT_LNG.latitude - 0.01)
                ),
                createRemoteLocation(
                    PALO_ALTO_LAT_LNG.copy(latitude = PALO_ALTO_LAT_LNG.latitude - 0.02)
                )
            ),
            0,
            0
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

    private fun createRemoteLocation(latLng: LatLng): RemoteLocation {
        return RemoteLocation(
            Coordinate(
                latLng.latitude,
                latLng.longitude
            ), ZonedDateTime.now().toIso()
        )
    }

}
