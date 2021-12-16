package com.hypertrack.android.api

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.mock.api.MockGraphQlApi
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.toIso
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class GraphQlApiClient(
    private val api: GraphQlApi,
    private val publishableKey: PublishableKey,
    private val deviceId: DeviceId,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
) {

    suspend fun getPlaceVisitsStats(days: List<DayRange>): Result<Map<DayRange, RemoteDayVisitsStats>> {
        return try {
            val daysMap = days.map { it.getGraphQlName() to it }.toMap()
            val queryBody = createPlaceVisitsQueryBody(days)

            api.getPlacesVisits(queryBody).let {
                if (it.isSuccessful) {
                    val response = it.body()!!.data
                    Success(response.mapKeys { (k, _) -> daysMap.getValue(k) })
                } else {
                    Failure(HttpException(it))
                }
            }
        } catch (e: Exception) {
            Failure(e)
        }
    }

    private fun createPlaceVisitsQueryBody(days: List<DayRange>): GraphQlApi.PlacesVisitsBody {
        val daysString = StringBuilder()
        days.forEach {
            daysString.append(
                """
                ${it.getGraphQlName()}: getDeviceHistory(
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

        val query = """
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

        return GraphQlApi.PlacesVisitsBody(query)
    }

}

data class DayRange(
    val localDate: LocalDate,
    private val zoneId: ZoneId
) {
    val zonedDateTimeStart: ZonedDateTime = ZonedDateTime.of(localDate, LocalTime.MIN, zoneId)
    val zonedDateTimeEnd: ZonedDateTime = ZonedDateTime.of(localDate, LocalTime.MAX, zoneId)

    override fun toString(): String {
        return "[${localDate}, zoneId=$zoneId]"
    }

    fun getGraphQlName(): String {
        return "day${localDate.toString().replace("-", "_")}"
    }
}

@JsonClass(generateAdapter = true)
data class GraphQlGeofence(
    @field:Json(name = "geofence_id") val geofence_id: String,
    @field:Json(name = "device_id") val deviceId: String?,
    @field:Json(name = "metadata") val metadata: String,
    @field:Json(name = "geometry") val _geometry: GraphQlPointGeometry,
    @field:Json(name = "radius") val radius: Int?,
    //this field is always null and have invalid format on graphql endpoint
//    @field:Json(name = "address") val address: String?,
) {

    @Transient
    val address: String? = null

    private fun getGeometry(): Geometry {
        return if (_geometry.center != null) {
            Point(_geometry.center)
        } else {
            Polygon(listOf(_geometry.vertices!!))
        }
    }

    val location: LatLng
        get() = LatLng(getGeometry().latitude, getGeometry().longitude)
    val type: String
        get() = getGeometry().type
}

@JsonClass(generateAdapter = true)
class GraphQlPointGeometry(
    val center: List<Double>?,
    val vertices: List<List<Double>>?,
)

@JsonClass(generateAdapter = true)
data class GraphQlGeofenceVisit(
    @field:Json(name = "id") val id: String,
//    @field:Json(name = "device_id") val deviceId: String,
    @field:Json(name = "arrival") val arrival: Arrival,
    @field:Json(name = "exit") val exit: Exit?,
    @field:Json(name = "route_to") val routeTo: RouteTo?,
    @field:Json(name = "geofence") val geofence: GraphQlGeofence,
) {
    companion object {
        //todo address
        //todo device_id
        const val FIELDS_QUERY = """
            id 
            arrival {
                recorded_at
            }
            exit {
                recorded_at
            }
            route_to {
                distance 
                duration
            }
            geofence {
                geofence_id
                geometry {
                    type
                    center
                    vertices
                }
                metadata
            }
        """
    }
}

@JsonClass(generateAdapter = true)
data class RemoteDayVisitsStats(
    @field:Json(name = "geofence_markers") val visits: List<GraphQlGeofenceVisit>,
    @field:Json(name = "drive_distance") val totalDriveDistanceMeters: Int
) {
    fun isEmpty(): Boolean {
        return visits.isEmpty() && totalDriveDistanceMeters == 0
    }
}
