package com.hypertrack.android.mock

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.api.ApiInterface
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceParams
import com.hypertrack.android.api.GeofenceResponse
import com.hypertrack.android.api.HistoryResponse
import com.hypertrack.android.api.IntegrationsResponse
import com.hypertrack.android.api.OrderBody
import com.hypertrack.android.api.Trip
import com.hypertrack.android.api.TripResponse
import com.hypertrack.android.api.VisitsResponse
import com.hypertrack.android.utils.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("BlockingMethodInNonBlockingContext")
class MockApi(val remoteApi: ApiInterface) : ApiInterface by remoteApi {

    private val fences = mutableListOf<Geofence>()
        .apply {
//            addAll(Injector.getMoshi().adapter(GeofenceResponse::class.java)
//                .fromJson(MockData.MOCK_GEOFENCES_JSON)!!.geofences)
        }

    override suspend fun completeTrip(tripId: String): Response<Unit> {
        delay(1000)
        return Response.success(Unit)
    }

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
            "",
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
        if (geohash == null) {
            //ordinal page
//            return remoteApi.getDeviceGeofences(
//                deviceId, geohash, paginationToken, includeArchived, includeMarkers, sortNearest
//            )
//
            return Response.success(
                GeofenceResponse(fences, null)
            )
//
//            return Response.success(
//                GeofenceResponse(listOf(MockData.createGeofence(polygon = true)), null)
//            )
//
//            return Response.success(
//                Injector.getMoshi().adapter(GeofenceResponse::class.java)
//                    .fromJson(MockData.MOCK_GEOFENCES_JSON)
//            )


        } else {
            //map page
            //Log.v("hypertrack-verbose", "getDeviceGeofences ${geohash}")
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
        }
    }

    override suspend fun getAllGeofencesVisits(
        deviceId: String,
        paginationToken: String?
    ): Response<VisitsResponse> {
//        return remoteApi.getAllGeofencesVisits(deviceId, paginationToken)
//
        return Response.success(
            VisitsResponse(
                listOf(
                    MockData.createGeofenceVisit()
                ), null
            )
        )
    }

    override suspend fun getHistory(
        deviceId: String,
        day: String,
        timezone: String
    ): Response<HistoryResponse> {
//        return remoteApi.getHistory(deviceId, day, timezone)
//
        delay((Math.random() * 1000 + 500).toLong())
        return Response.success(MockData.MOCK_HISTORY_RESPONSE)
    }

    override suspend fun getHistoryForPeriod(
        deviceId: String,
        from: String,
        to: String
    ): Response<HistoryResponse> {
//        return remoteApi.getHistoryForPeriod(deviceId, from, to)

        delay((Math.random() * 1000 + 500).toLong())
        return Response.success(MockData.MOCK_HISTORY_RESPONSE)
    }

    override suspend fun getIntegrations(
        query: String?,
        limit: Int?
    ): Response<IntegrationsResponse> {
        val hasIntegrations = false
        if (hasIntegrations) {
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
        } else {
            return Response.success(IntegrationsResponse(listOf()))
        }
    }

    override suspend fun getTrips(
        deviceId: String,
        paginationToken: String
    ): Response<TripResponse> {
//        return Response.success(TripResponse(listOf(MockData.createTrip()), null))

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
