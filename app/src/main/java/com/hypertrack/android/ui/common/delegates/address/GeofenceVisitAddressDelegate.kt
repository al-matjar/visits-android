package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GeofenceVisitAddressDelegate(
    private val osUtilsProvider: OsUtilsProvider
) {
    fun shortAddress(visit: LocalGeofenceVisit): String {
        return visit.address
            ?: visit.metadata?.address
            ?: visit.location?.let {
                osUtilsProvider.getPlaceFromCoordinates(
                    it.latitude,
                    it.longitude
                )?.toAddressString(short = true)
            }
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }
}
