package com.hypertrack.android.api

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.models.RemoteGeofence
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.SimpleResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
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
            // todo handle EOFException: End of input if there is no such geofence
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
        return try {
            val response = api.getTrips(
                deviceId = deviceId.value,
                paginationToken = page
            )
            if (response.isSuccessful) {
                response.body()?.trips?.filterNot {
                    it.id.isNullOrEmpty()
                } ?: emptyList()
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            throw e
        }
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
