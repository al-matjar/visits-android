package com.hypertrack.android.ui.common.util

import android.location.Location
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import kotlin.math.round

object LocationUtils {
    fun distanceMeters(latLng: LatLng?, latLng1: LatLng?): Int? {
        try {
            if (latLng != null && latLng1 != null
                && !(latLng.latitude == 0.0 && latLng.longitude == 0.0)
                && !(latLng1.latitude == 0.0 && latLng1.longitude == 0.0)
            ) {
                val res = FloatArray(3)
                android.location.Location.distanceBetween(
                    latLng.latitude,
                    latLng.longitude,
                    latLng1.latitude,
                    latLng1.longitude,
                    res
                );
                return res[0].toInt()
            } else {
                return null
            }
        } catch (_: Exception) {
            return null
        }
    }
}

fun Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun Double.roundToSign(n: Int): Double {
    return round(this * 10.0.pow(n)) / (10.0.pow(n))
}

fun LatLng.format(): String {
    return "${latitude.roundToSign(5)}, ${longitude.roundToSign(5)}"
}

fun LatLng.copy(latitude: Double? = null, longitude: Double? = null): LatLng {
    return LatLng(latitude ?: this.latitude, longitude ?: this.longitude)
}

fun LatLng.getGeoHash(charsCount: Int = 4): GeoHash {
    return GeoHash(latitude, longitude, charsCount)
}



