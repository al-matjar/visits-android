package com.hypertrack.android.ui.common.delegates.address

import android.location.Address
import com.google.android.libraries.places.api.model.Place
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.getAddressString
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class GooglePlaceAddressDelegate(
    private val osUtilsProvider: OsUtilsProvider
) {

    fun displayAddress(place: Place): String {
        return place.getAddressString(strictMode = false)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

    fun displayAddress(address: Address?): String {
        return address?.toAddressString(disableCoordinatesFallback = true)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

    fun strictAddress(address: Address): String? {
        return address.toAddressString(strictMode = true)
    }

    fun strictAddress(place: Place): String? {
        return place.getAddressString(strictMode = true)
    }

}
