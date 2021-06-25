package com.hypertrack.android.api

import android.util.Log
import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.Meter
import com.hypertrack.android.utils.MockData
import com.hypertrack.android.utils.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Query
import java.lang.RuntimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
class MockApi(val remoteApi: ApiInterface) : ApiInterface by remoteApi {

//    private val fences = mutableListOf<Geofence>()
private val fences = Injector.getMoshi().adapter(GeofenceResponse::class.java)
    .fromJson(MockData.MOCK_GEOFENCES_JSON)!!.geofences.toMutableList()

    override suspend fun createGeofences(
        deviceId: String,
        params: GeofenceParams
    ): Response<List<Geofence>> {
//        return remoteApi.createGeofences(deviceId, params)
        val created = Geofence(
            "",
            "00000000-0000-0000-0000-000000000000",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            params.geofences.first().metadata,
            params.geofences.first().geometry,
            null,
            100,
            false,
        )
        fences.add(created)
        return Response.success(listOf(created))
    }

    @Suppress("UNREACHABLE_CODE")
    override suspend fun getDeviceGeofences(
        deviceId: String,
        geohash: String?,
        paginationToken: String?,
        includeArchived: Boolean,
        includeMarkers: Boolean,
        sortNearest: Boolean
    ): Response<GeofenceResponse> {
        return Response.success(
            GeofenceResponse(fences, null)
        )
//        return Response.success(
//            Injector.getMoshi().adapter(GeofenceResponse::class.java)
//                .fromJson(MockData.MOCK_GEOFENCES_JSON)
//        )

        if (geohash != null) {
//            Log.v("hypertrack-verbose", "getDeviceGeofences ${geohash}")
            val res = withContext(Dispatchers.IO) {
//                delay((Math.random() * 1000).toLong())

                val page = (paginationToken?.split("/")?.get(0)?.toInt() ?: 0) + 1
                val totalPages = 1
//        val totalPages =
//            (paginationToken?.split("/")?.get(1)?.toInt()) ?: (3 + (Math.random() * 3f).toInt())

//        if (Math.random() > 0.8f /*&& *//*page > 1*/) {
//            throw RuntimeException("${geohash} ${page}")
//        }

                val gh = GeoHash(geohash)
                val res = GeofenceResponse(
                    (0..100).map {
                        MockData.createGeofence(
                            0,
//                            lat = gh.boundingBox.maxLat - 0.005 * page,
//                            lon = gh.boundingBox.maxLon - 0.005 * page
                            lat = gh.boundingBox.let { it.maxLat - Math.random() * (it.maxLat - it.minLat) },
                            lon = gh.boundingBox.let { it.maxLon - Math.random() * (it.maxLon - it.minLon) }
                        )
                    },
                    if (page < totalPages) {
                        "${page}/${totalPages}"
                    } else {
                        null
                    }
                )

                res
            }
            return Response.success(res)
        } else {
            return Response.success(
                GeofenceResponse(fences, null)
            )
        }
    }

    override suspend fun getIntegrations(
        query: String?,
        limit: Int?
    ): Response<IntegrationsResponse> {
        return Response.success(
            Injector.getMoshi().adapter(IntegrationsResponse::class.java)
                .fromJson(MockData.MOCK_INTEGRATIONS_RESPONSE)!!.let {
                    if (query != null) {
                        it.copy(data = it.data.filter { it.name?.contains(query.toString()) == true })
                    } else {
                        it
                    }
                }
        )
    }

    override suspend fun getTrips(
        deviceId: String,
        paginationToken: String
    ): Response<TripResponse> {
        return Response.success(
            Injector.getMoshi().adapter(TripResponse::class.java)
                .fromJson(MockData.MOCK_TRIPS_JSON)
        )
    }

    override suspend fun completeOrder(tripId: String, orderId: String): Response<Void> {
        delay(500)
        return Response.success(null)
    }

    override suspend fun cancelOrder(tripId: String, orderId: String): Response<Void> {
        delay(500)
        return Response.success(null)
    }

    override suspend fun updateOrder(
        tripId: String,
        orderId: String,
        order: OrderBody
    ): Response<Trip> {
        delay(500)
        return Response.success(
            Injector.getMoshi().adapter(TripResponse::class.java)
                .fromJson(MockData.MOCK_TRIPS_JSON)!!.trips.first()
        )
    }
}