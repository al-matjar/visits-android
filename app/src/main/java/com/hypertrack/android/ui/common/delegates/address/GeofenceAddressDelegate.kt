package com.hypertrack.android.ui.common.delegates.address

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.SHORT_ADDRESS_LIMIT
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GeofenceAddressDelegate(
    private val geocodingInteractor: GeocodingInteractor,
) {

    suspend fun shortAddress(geofence: Geofence): String? {
        return geofence.address?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        }
            ?: geocodingInteractor.getPlaceFromCoordinates(geofence.latLng)
                ?.toAddressString(short = true)
    }

    suspend fun fullAddress(geofence: Geofence): String? {
        return geofence.address?.nullIfBlank()
            ?: geocodingInteractor.getPlaceFromCoordinates(geofence.latLng)?.toAddressString()
    }


}
