package com.hypertrack.android.api

import android.graphics.Bitmap
import android.util.Log
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.repository.AccessTokenRepository
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MockData
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class ApiClient(
    private val accessTokenRepository: AccessTokenRepository,
    private val baseUrl: String,
    private val deviceId: String,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
) : AbstractBackendProvider {

    @Suppress("unused")
    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val remoteApi: ApiInterface = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(Injector.getMoshi()))
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor {
                    val response = it.proceed(it.request())
                    crashReportsProvider.log("${it.request().method} ${it.request().url.encodedPath} ${response.code}")
                    response
                }
                .authenticator(AccessTokenAuthenticator(accessTokenRepository))
                .addInterceptor(AccessTokenInterceptor(accessTokenRepository))
                .addInterceptor(UserAgentInterceptor())
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS).apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(loggingInterceptor)
                    }
                }.build()
        )
        .build()
        .create(ApiInterface::class.java)

    private val api = if (MyApplication.MOCK_MODE.not()) {
        remoteApi
    } else {
        MockApi(remoteApi)
    }

    suspend fun clockIn() = api.clockIn(deviceId)

    suspend fun clockOut() = api.clockOut(deviceId)

    suspend fun getGeofences(
        paginationToken: String?,
        geoHash: String? = null
    ): GeofenceResponse {
        try {
//            Log.v("hypertrack-verbose", "getGeofences ${paginationToken.hashCode()}")
            val response = api.getDeviceGeofences(
                paginationToken = paginationToken,
                deviceId = deviceId,
                geohash = geoHash
            )
            if (response.isSuccessful) {
                return response.body()!!
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
        metadata: GeofenceMetadata
    ): Response<List<Geofence>> {
        return api.createGeofences(
            deviceId,
            GeofenceParams(
                setOf(
                    GeofenceProperties(
                        Point(latitude = latitude, longitude = longitude),
                        metadata.toMap(moshi),
                        radius
                    )
                ), deviceId
            )
        )
    }

    suspend fun getAllGeofencesVisits(
        paginationToken: String?,
    ): VisitsResponse {
        try {
            val response = api.getAllGeofencesVisits(
                deviceId = deviceId,
                paginationToken = paginationToken,
            )
            if (response.isSuccessful) {
                return response.body()!!
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getTrips(page: String = ""): List<Trip> {
        try {
            val response = api.getTrips(deviceId, page)
            if (response.isSuccessful) {
                // Log.v(TAG, "Got response ${response.body()}")
                return response.body()?.trips?.filterNot {
                    it.destination == null || it.tripId.isEmpty()
                }
                    ?: emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Got exception while trying to refresh trips $e")
            throw e
        }
        return emptyList()
    }

    suspend fun updateOrderMetadata(
        orderId: String,
        tripId: String,
        metadata: Metadata
    ): Response<Trip> {
        try {
            return api.updateOrder(
                orderId = orderId,
                tripId = tripId,
                order = OrderBody(metadata = metadata.toMap())
            )
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun uploadImage(filename: String, image: Bitmap) {
        try {
            val response = api.persistImage(deviceId, EncodedImage(filename, image))
            if (response.isSuccessful) {
                // Log.v(TAG, "Got post image response ${response.body()}")
            } else {
                throw HttpException(response)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Got exception $e uploading image")
            throw e
        }
    }

    suspend fun getHistory(day: LocalDate, timezone: ZoneId): HistoryResult {
        return if (MyApplication.MOCK_MODE.not()) {
            try {
                with(
                    api.getHistory(
                        deviceId,
                        day.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        timezone.id
                    )
                ) {
                    if (isSuccessful) {
                        return body().asHistory()
                    } else {
                        return HistoryError(HttpException(this))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Got exception $e fetching device history")
                return HistoryError(e)
            }
        } else {
            //todo inject mock api client
            MockData.MOCK_HISTORY
        }
    }

    suspend fun getHistory(from: ZonedDateTime, to: ZonedDateTime): HistoryResult {
        try {
            with(
                api.getHistoryForPeriod(
                    deviceId,
                    from.format(DateTimeFormatter.ISO_INSTANT),
                    to.format(DateTimeFormatter.ISO_INSTANT)
                )
            ) {
                if (isSuccessful) {
                    return body().asHistory()
                } else {
                    return HistoryError(HttpException(this))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Got exception $e fetching device history")
            return HistoryError(e)
        }
    }

    override suspend fun createTrip(tripParams: TripParams): ShareableTripResult {
        return try {
            with(api.createTrip(tripParams)) {
                if (isSuccessful) {
                    val trip = body()!!
                    ShareableTripSuccess(
                        trip.views.shareUrl,
                        trip.views.embedUrl,
                        trip.tripId,
                        trip.estimate?.route?.remainingDuration
                    )
                } else CreateTripError(HttpException(this))
            }
        } catch (t: Throwable) {
            CreateTripError(t)
        }
    }

    suspend fun createTrip(latLng: LatLng, address: String?): Trip {
        try {
            val res = api.createTrip(
                TripParams(
                    deviceId = deviceId,
                    orders = listOf(
                        OrderParams(
                            orderId = UUID.randomUUID().toString(),
                            destination = TripDestination(
                                geometry = Point(
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude,
                                ),
                                address = address,
                                radius = null
                            )
                        )
                    )
                )
            )
            if (res.isSuccessful) {
                val trip = res.body()!!
                return trip
            } else {
                throw HttpException(res)
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun addOrderToTrip(
        tripId: String,
        orderCreationParams: OrderCreationParams
    ): Trip {
        try {
            with(
                api.addOrderToTrip(
                    tripId,
                    AddOrderBody(
                        deviceId = deviceId,
                        orderCreationParams = listOf(orderCreationParams)
                    )
                )
            ) {
                if (isSuccessful) {
                    return body()!!
                } else {
                    throw HttpException(this)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun completeTrip(tripId: String): TripCompletionResult {
        return try {
            with(api.completeTrip(tripId)) {
                if (isSuccessful) TripCompletionSuccess
                else TripCompletionError(HttpException(this))
            }
        } catch (t: Throwable) {
            TripCompletionError(t)
        }
    }

    suspend fun completeOrder(orderId: String, tripId: String): OrderCompletionResponse {
        try {
            val res = api.completeOrder(tripId = tripId, orderId = orderId)
            if (res.isSuccessful) {
                return OrderCompletionSuccess
            } else {
                if (res.code() == 409) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val order = withContext(Dispatchers.IO) {
                        moshi.adapter(Order::class.java)
                            .fromJson(res.errorBody()!!.string())
                    }
                    when (order!!.status) {
                        OrderStatus.COMPLETED -> return OrderCompletionCompleted
                        OrderStatus.CANCELED -> return OrderCompletionCanceled
                        else -> return OrderCompletionFailure(HttpException(res))
                    }
                } else {
                    return OrderCompletionFailure(HttpException(res))
                }
            }
        } catch (e: Exception) {
            return OrderCompletionFailure(e)
        }
    }

    suspend fun cancelOrder(orderId: String, tripId: String): OrderCompletionResponse {
        try {
            val res = api.cancelOrder(tripId = tripId, orderId = orderId)
            if (res.isSuccessful) {
                return OrderCompletionSuccess
            } else {
                if (res.code() == 409) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val order = withContext(Dispatchers.IO) {
                        moshi.adapter(Order::class.java)
                            .fromJson(res.errorBody()!!.string())
                    }
                    when (order!!.status) {
                        OrderStatus.COMPLETED -> return OrderCompletionCompleted
                        OrderStatus.CANCELED -> return OrderCompletionCanceled
                        else -> return OrderCompletionFailure(HttpException(res))
                    }
                } else {
                    return OrderCompletionFailure(HttpException(res))
                }
            }
        } catch (e: Exception) {
            return OrderCompletionFailure(e)
        }
    }

    suspend fun getIntegrations(query: String? = null, limit: Int? = null): List<Integration> {
        val res = api.getIntegrations(query, limit)
        if (res.isSuccessful) {
            return res.body()!!.data
        } else {
            throw HttpException(res)
        }
    }

    suspend fun getImageBase64(imageId: String): String? {
        try {
            val res = api.getImage(deviceId = deviceId, imageId = imageId)
            if (res.isSuccessful) {
                return res.body()?.data
            } else {
                throw HttpException(res)
            }
        } catch (e: Exception) {
            //todo handle
            return null
        }
    }

    companion object {
        const val TAG = "ApiClient"
    }

}

private fun HistoryResponse?.asHistory(): HistoryResult {
    return if (this == null) {
        HistoryError(null)
    } else {
        History(
            Summary(
                distance,
                duration,
                distance,
                driveDuration ?: 0,
                stepsCount ?: 0,
                walkDuration,
                stopDuration,
            ),
            locations.coordinates.map {
                Location(
                    latitude = it.latitude,
                    longitude = it.longitude
                ) to it.timestamp
            },
            markers.map { it.asMarker() }
        )
    }
}

fun HistoryMarker.asMarker(): Marker {
    return when (this) {
        is HistoryStatusMarker -> asStatusMarker()
        is HistoryTripMarker ->
            GeoTagMarker(
                MarkerType.GEOTAG,
                data.recordedAt,
                data.location?.asLocation(),
                data.metadata ?: emptyMap()
            )
        is HistoryGeofenceMarker -> asGeofenceMarker()
        else -> throw IllegalArgumentException("Unknown marker type $type")
    }
}

private fun HistoryGeofenceMarker.asGeofenceMarker(): Marker {
    return GeofenceMarker(
        MarkerType.GEOFENCE_ENTRY,
        data.arrival.location.recordedAt,
        data.arrival.location.geometry?.asLocation(),
        data.geofence.metadata ?: emptyMap(),
        data.arrival.location.geometry?.asLocation(),
        data.exit?.location?.geometry?.asLocation(),
        data.arrival.location.recordedAt,
        data.exit?.location?.recordedAt
    )
}


private fun HistoryTripMarkerLocation.asLocation() = Location(
    latitude = coordinates[1],
    longitude = coordinates[0]
)

fun Geometry.asLocation() =
    Location(
        latitude = latitude,
        longitude = longitude
    )

private fun HistoryStatusMarker.asStatusMarker() = StatusMarker(
    MarkerType.STATUS,
    data.start.recordedAt,
    data.start.location?.geometry?.asLocation(),
    data.start.location?.geometry?.asLocation(),
    data.end.location?.geometry?.asLocation(),
    data.start.recordedAt,
    data.end.recordedAt,
    data.start.location?.recordedAt,
    data.end.location?.recordedAt,
    when (data.value) {
        "inactive" -> Status.INACTIVE
        "active" -> when (data.activity) {
            "stop" -> Status.STOP
            "drive" -> Status.DRIVE
            "walk" -> Status.WALK
            else -> Status.UNKNOWN
        }
        else -> Status.UNKNOWN

    },
    data.duration,
    data.distance,
    data.steps,
    data.address,
    data.reason
)

sealed class OrderCompletionResponse
object OrderCompletionSuccess : OrderCompletionResponse()
object OrderCompletionCanceled : OrderCompletionResponse()
object OrderCompletionCompleted : OrderCompletionResponse()
class OrderCompletionFailure(val exception: Exception) : OrderCompletionResponse()