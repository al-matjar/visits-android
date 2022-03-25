package com.hypertrack.android.api

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.models.RemoteGeofence
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ApiClient(
    private val api: ApiInterface,
    private val deviceId: DeviceId,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider
) {

    suspend fun getGeofences(
        paginationToken: String?,
        geoHash: String? = null
    ): GeofenceResponse {
        try {
//            Log.v("hypertrack-verbose", "getGeofences ${paginationToken.hashCode()}")
            val response = api.getDeviceGeofences(
                paginationToken = paginationToken,
                deviceId = deviceId.value,
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

    suspend fun getGeofence(geofenceId: String): RemoteGeofence {
        try {
            val metadataResponse = api.getGeofenceMetadata(geofenceId).apply {
                if (!isSuccessful) throw HttpException(this)
            }
            val visitsResponse = api.getGeofenceVisits(geofenceId).apply {
                if (!isSuccessful) throw HttpException(this)
            }
            val visitsBody = visitsResponse.body()!!
            return metadataResponse.body()!!.copy(
                marker = GeofenceMarkersResponse(
                    visitsBody.visits,
                    visitsBody.paginationToken,
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
        metadata: GeofenceMetadata
    ): Response<List<RemoteGeofence>> {
        return api.createGeofences(
            deviceId = deviceId.value,
            GeofenceParams(
                setOf(
                    GeofenceProperties(
                        Point(latitude = latitude, longitude = longitude),
                        metadata.toMap(moshi),
                        radius
                    )
                ),
                deviceId = deviceId.value
            )
        )
    }

    suspend fun getTrips(page: String = ""): List<Trip> {
        try {
            val response = api.getTrips(
                deviceId = deviceId.value,
                paginationToken = page
            )
            if (response.isSuccessful) {
                return response.body()?.trips?.filterNot {
                    it.id.isNullOrEmpty()
                } ?: emptyList()
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
            val response = api.persistImage(
                deviceId = deviceId.value,
                EncodedImage(filename, image)
            )
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
        try {
            with(
                api.getHistory(
                    deviceId = deviceId.value,
                    day = day.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    timezone = timezone.id
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

    suspend fun createTrip(latLng: LatLng, address: String?): Trip {
        try {
            val res = api.createTrip(
                TripParams(
                    deviceId = deviceId.value,
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
                        deviceId = deviceId.value,
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

    suspend fun completeTrip(tripId: String): SimpleResult {
        return try {
            with(api.completeTrip(tripId)) {
                if (isSuccessful) JustSuccess
                else JustFailure(HttpException(this))
            }
        } catch (e: Exception) {
            JustFailure(e)
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
                        moshi.adapter(RemoteOrder::class.java)
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
                        moshi.adapter(RemoteOrder::class.java)
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

    suspend fun snoozeOrder(orderId: String, tripId: String): SimpleResult {
        return try {
            val res = api.snoozeOrder(tripId = tripId, orderId = orderId)
            if (res.isSuccessful) {
                JustSuccess
            } else {
                JustFailure(HttpException(res))
            }
        } catch (e: Exception) {
            JustFailure(e)
        }
    }

    suspend fun unsnoozeOrder(orderId: String, tripId: String): SimpleResult {
        return try {
            val res = api.unsnoozeOrder(tripId = tripId, orderId = orderId)
            if (res.isSuccessful) {
                JustSuccess
            } else {
                JustFailure(HttpException(res))
            }
        } catch (e: Exception) {
            JustFailure(e)
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
            val res = api.getImage(deviceId = deviceId.value, imageId = imageId)
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

