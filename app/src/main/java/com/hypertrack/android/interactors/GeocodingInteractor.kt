package com.hypertrack.android.interactors

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.NetworkOnMainThreadException
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.suspendCoroutine

class GeocodingInteractor(
    private val context: Context,
    private val crashReportsProvider: CrashReportsProvider
) {

    private val geocoder = Geocoder(context)

    @WorkerThread
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getPlaceFromCoordinates(latLng: LatLng): Address? = withTimeoutOrNull(
        GEOCODING_RESPONSE_TIMEOUT
    ) {
        withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1
                )?.get(0)
            } catch (e: Exception) {
                crashReportsProvider.logException(e)
                null
            }
        }
    }

    companion object {
        const val GEOCODING_RESPONSE_TIMEOUT = 1000L
    }

}
