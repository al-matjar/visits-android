package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.GeofenceMarkersResponse
import com.hypertrack.android.api.Geometry
import com.hypertrack.android.api.Polygon
import com.hypertrack.android.api.models.RemoteGeofence
import com.hypertrack.android.mock.MockData.moshi
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.GeofenceId
import com.hypertrack.android.models.local.LocalGeofenceVisit
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// don't delete unusable code here, it can be used in main sources to mock responses
object GeofenceMockData {

    fun createGeofence(
        location: LatLng = MockData.PALO_ALTO_LAT_LNG
    ): Geofence {
        return Geofence(
            id = GeofenceId("geofence_id_${Math.random()}"),
            name = "",
            address = "",
            radius = 500,
            location = location,
            createdAt = ZonedDateTime.now(),
            isPolygon = false,
            polygon = null,
            integration = null,
            metadata = mapOf(),
            visits = listOf()
        )
    }


    fun createRemoteGeofence(
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
                "coordinates": [$lon, $lat]
            }
            """.let {
                moshi.adapter(Geometry::class.java)
                    .fromJson(it)!!
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

}
