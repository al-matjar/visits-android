package com.hypertrack.android.ui.common.delegates

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.api.GraphQlGeofenceVisit
import com.hypertrack.android.api.asLocation
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.util.format

import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.common.util.nullIfEmpty
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetimeFromString
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.logistics.android.github.R

class OrderAddressDelegate(
    val osUtilsProvider: OsUtilsProvider,
    val datetimeFormatter: DatetimeFormatter
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
                R.string.order_scheduled_at,
                datetimeFormatter.formatDatetime(it)
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

    fun shortAddress(visit: LocalGeofenceVisit): String {
        return visit.address
            ?: visit.metadata?.address
            ?: visit.location.let {
                osUtilsProvider.getPlaceFromCoordinates(
                    it.latitude,
                    it.longitude
                )?.toAddressString(short = true)
            }
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}

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

class GraphQlGeofenceVisitAddressDelegate(
    private val osUtilsProvider: OsUtilsProvider
) {

    fun displayAddress(visit: GraphQlGeofenceVisit): String {
        return visit.geofence.address
            ?: visit.geofence.location.let {
                osUtilsProvider.getPlaceFromCoordinates(it)
            }?.toAddressString(strictMode = false, short = true, disableCoordinatesFallback = true)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}

fun String?.parseNominatimAddress(): String? {
    return this?.let {
        split(",").map { it.trim() }.take(2).joinToString(", ")
    }
}

fun Address.toAddressString(
    strictMode: Boolean = false,
    short: Boolean = false,
    disableCoordinatesFallback: Boolean = false,
): String? {
    if (strictMode && thoroughfare == null) {
        return null
    }

    val firstPart = if (short) null else locality ?: ""

    val secondPart = if (!short) {
        //long
        if (thoroughfare != null) {
            "$thoroughfare${subThoroughfare.wrapIfPresent(start = ", ")}"
        } else {
            if (!disableCoordinatesFallback) {
                LatLng(latitude, longitude).format()
            } else {
                ""
            }
        }
    } else {
        //short
        if (thoroughfare != null) {
            "$thoroughfare${subThoroughfare.wrapIfPresent(start = ", ")}"
        } else {
            if (!disableCoordinatesFallback) {
                LatLng(latitude, longitude).format()
            } else {
                locality.wrapIfPresent()
            }
        }
    }

    return listOf(firstPart, secondPart)
        .filter { !it.isNullOrBlank() }
        .joinToString(", ")
        .nullIfEmpty()
}

fun Place.getAddressString(
    strictMode: Boolean = false
): String? {
    val locality =
        addressComponents?.asList()?.firstOrNull { "locality" in it.types }?.name
            ?: addressComponents?.asList()
                ?.firstOrNull { "administrative_area_level_1" in it.types }?.name
            ?: addressComponents?.asList()
                ?.firstOrNull { "administrative_area_level_2" in it.types }?.name
            ?: addressComponents?.asList()?.firstOrNull { "political" in it.types }?.name

    val thoroughfare =
        addressComponents?.asList()
            ?.firstOrNull { "route" in it.types }
            ?.name


    var subThoroughfare =
        addressComponents?.asList()?.firstOrNull { "street_number" in it.types }?.name
    addressComponents?.asList()
        ?.firstOrNull { "subpremise" in it.types }
        ?.name.let {
            subThoroughfare += " $it"
        }

    val parts = listOfNotNull(locality, thoroughfare, subThoroughfare)

    return if (parts.isEmpty() || (strictMode && (thoroughfare == null || subThoroughfare == null))) {
        null
    } else {
        parts.joinToString(", ")
    }
}


fun String?.wrapIfPresent(start: String? = null, end: String? = null): String {
    return this?.let { "${start.wrapIfPresent()}$it${end.wrapIfPresent()}" } ?: ""
}

const val SHORT_ADDRESS_LIMIT = 50