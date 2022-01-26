package com.hypertrack.android.ui.common.util

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Location
import kotlin.math.pow
import kotlin.math.round

object LocationUtils {
    fun distanceMeters(location: Location?, location1: Location?): Int? {
        try {
            if (location != null && location1 != null
                && !(location.latitude == 0.0 && location.longitude == 0.0)
                && !(location1.latitude == 0.0 && location1.longitude == 0.0)
            ) {
                val res = FloatArray(3)
                android.location.Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    location1.latitude,
                    location1.longitude,
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

fun android.location.Location.toLatLng(): LatLng {
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



