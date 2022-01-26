package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.SHORT_ADDRESS_LIMIT
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GeofenceAddressDelegate(val osUtilsProvider: OsUtilsProvider) {

    fun shortAddress(geofence: LocalGeofence): String {
        return geofence.address?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        } ?: osUtilsProvider.getPlaceFromCoordinates(
            geofence.latLng.latitude,
            geofence.latLng.longitude
        )?.toAddressString(short = true)
        ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

    fun fullAddress(geofence: LocalGeofence): String {
        return geofence.address?.nullIfBlank()
            ?: osUtilsProvider.getPlaceFromCoordinates(
                geofence.latLng.latitude,
                geofence.latLng.longitude
            )?.toAddressString()
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }


}
