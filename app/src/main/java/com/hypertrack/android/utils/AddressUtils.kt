package com.hypertrack.android.utils

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.hypertrack.android.ui.common.util.format

import com.hypertrack.android.ui.common.util.nullIfEmpty

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
