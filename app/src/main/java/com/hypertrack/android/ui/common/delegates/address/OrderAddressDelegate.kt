package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.SHORT_ADDRESS_LIMIT
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class OrderAddressDelegate(
    val osUtilsProvider: OsUtilsProvider,
    val dateTimeFormatter: DateTimeFormatter
) {

    //todo nominatim first two parts
    //used as order name in list
    fun shortAddress(order: LocalOrder): String {
        return order.destinationAddress?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        } ?: osUtilsProvider.getPlaceFromCoordinates(
            order.destinationLatLng.latitude,
            order.destinationLatLng.longitude
        )?.toAddressString(short = true, disableCoordinatesFallback = true)
        ?: order.scheduledAt?.let {
            osUtilsProvider.stringFromResource(
                R.string.order_scheduled_at_template,
                dateTimeFormatter.formatDateTime(it)
            )
        }
        ?: osUtilsProvider.stringFromResource(R.string.order_address_not_available)
    }

    fun fullAddress(order: LocalOrder): String {
        return order.destinationAddress?.nullIfBlank()
            ?: osUtilsProvider.getPlaceFromCoordinates(
                order.destinationLatLng.latitude,
                order.destinationLatLng.longitude
            )?.toAddressString(disableCoordinatesFallback = true)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}
