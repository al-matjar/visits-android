package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.SHORT_ADDRESS_LIMIT
import com.hypertrack.android.utils.toAddressString

class GeofenceAddressDelegate(
    private val geocodingInteractor: GeocodingInteractor,
) {

    suspend fun shortAddress(geofence: Geofence): String? {
        return geofence.address?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        }
            ?: geocodingInteractor.getPlaceFromCoordinates(geofence.location)
                ?.toAddressString(short = true)
    }

    suspend fun fullAddress(geofence: Geofence): String? {
        return geofence.address?.nullIfBlank()
            ?: geocodingInteractor.getPlaceFromCoordinates(geofence.location)?.toAddressString()
    }


}
