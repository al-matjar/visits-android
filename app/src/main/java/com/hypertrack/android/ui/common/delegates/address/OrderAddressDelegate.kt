package com.hypertrack.android.ui.common.delegates.address

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.SHORT_ADDRESS_LIMIT
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.logistics.android.github.R

class OrderAddressDelegate(
    private val geocodingInteractor: GeocodingInteractor,
    private val resourceProvider: ResourceProvider,
    private val dateTimeFormatter: DateTimeFormatter
) {

    // todo nominatim first two parts
    // used as order name in list
    suspend fun getShortAddress(order: RemoteOrder): String? {
        return order.destination.address?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        }
            ?: geocodingInteractor.getPlaceFromCoordinates(
                order.destination.geometry.toLatLng()
            )?.toAddressString(short = true, disableCoordinatesFallback = true)
            ?: order.scheduledAt?.let {
                resourceProvider.stringFromResource(
                    R.string.order_scheduled_at_template,
                    dateTimeFormatter.formatDateTime(dateTimeFromString(it))
                )
            }
    }

    suspend fun getFullAddress(order: RemoteOrder): String? {
        return order.destination.address?.nullIfBlank()
            ?: geocodingInteractor.getPlaceFromCoordinates(order.destination.geometry.toLatLng())
                ?.toAddressString(disableCoordinatesFallback = true)
    }

}
