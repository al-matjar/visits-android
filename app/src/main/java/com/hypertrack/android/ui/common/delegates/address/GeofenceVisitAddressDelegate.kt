package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GeofenceVisitAddressDelegate(
    private val geocodingInteractor: GeocodingInteractor,
    private val resourceProvider: ResourceProvider,
) {

    suspend fun shortAddress(visit: LocalGeofenceVisit): String {
        return visit.address
            ?: visit.metadata?.address
            ?: visit.location?.let {
                geocodingInteractor.getPlaceFromCoordinates(it)
                    ?.toAddressString(short = true)
            }
            ?: resourceProvider.stringFromResource(R.string.address_not_available)
    }
}
