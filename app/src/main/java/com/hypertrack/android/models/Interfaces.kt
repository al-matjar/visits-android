package com.hypertrack.android.models

import androidx.annotation.FloatRange
import com.hypertrack.android.api.TripParams


interface AbstractBackendProvider : HomeManagementApi, TripManagementApi
interface TripManagementApi {
    suspend fun createTrip(tripParams: TripParams) : ShareableTripResult
    suspend fun finishTrip(tripId: String) : TripCompletionResult
}

interface HomeManagementApi {
    fun getHomeGeofenceLocation(resultHandler: ResultHandler<GeofenceLocation?>)
    fun updateHomeGeofence(homeLocation: GeofenceLocation, resultHandler: ResultHandler<Void?>)
}

interface ResultHandler<T> {
    fun onResult(result: T)
    fun onError(error: Exception)
}

data class GeofenceLocation(val latitude: Double, val longitude: Double)

sealed class ShareableTripResult
class ShareableTrip(val shareUrl: String, val embedUrl: String, val tripId: String, val remainingDuration: Int?) : ShareableTripResult()
class CreateTripError(val error: Throwable?) : ShareableTripResult()

sealed class TripCompletionResult
object TripCompletionSuccess : TripCompletionResult()
class TripCompletionError(val error: Throwable?) : TripCompletionResult()


class TripConfig internal constructor(
        val latitude: Double?,
        val longitude: Double?,
        val deviceId: String
) {

    fun getRequestBody(): String {
        return if (latitude == null || longitude == null) {
            "{\"device_id\":\"${deviceId}\"}"
        } else {
            "{\"destination\":{\"geometry\":{\"coordinates\":[${longitude},${latitude}],\"type\":\"Point\"}},\"device_id\":\"${deviceId}\"}"
        }
    }

    class Builder {
        private var destinationLatitude: Double? = null
        private var destinationLongitude: Double? = null
        private var deviceId : String? = null

        /**
         * @param latitude of trip's destination. Negative values are for southern hemisphere.
         * Unless both latitude and longitude are set values are ignored.
         * @see .setDestinationLongitude
         */
        fun setDestinationLatitude(@FloatRange(from = -90.0, to = 90.0) latitude: Double): Builder {
            destinationLatitude = latitude
            return this
        }

        /**
         * @param longitude of trip's destination. Negative values are for western hemisphere.
         * Unless both latitude and longitude are set values are ignored.
         * @see .setDestinationLatitude
         */
        fun setDestinationLongitude(@FloatRange(from = -180.0, to = 180.0) longitude: Double): Builder {
            destinationLongitude = longitude
            return this
        }

        fun setDeviceId(deviceId: String): Builder {
            this.deviceId = deviceId
            return this
        }

        fun build(): TripConfig {
            requireNotNull(deviceId)
            return TripConfig(
                destinationLatitude,
                destinationLongitude,
                deviceId ?: throw NullPointerException("device id can't be null")
            )
        }
    }

}