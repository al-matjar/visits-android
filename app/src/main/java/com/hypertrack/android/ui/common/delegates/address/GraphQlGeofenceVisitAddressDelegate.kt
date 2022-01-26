package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GraphQlGeofenceVisitAddressDelegate(
    private val osUtilsProvider: OsUtilsProvider
) {

    fun displayAddress(visit: GraphQlGeofenceVisit): String {
        return visit.geofence.address
            ?: visit.geofence.location?.let {
                osUtilsProvider.getPlaceFromCoordinates(it)
            }?.toAddressString(strictMode = false, short = true, disableCoordinatesFallback = true)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}
