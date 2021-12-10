package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationGenerator {

    fun generate(callback: (LatLng) -> Unit): Job {
        return GlobalScope.launch {
            var fraction = 0.0
            while (true) {
                delay(1000)
                callback.invoke(
                    SphericalUtil.interpolate(
                        MockData.PALO_ALTO_LAT_LNG,
                        MockData.PALO_ALTO_LAT_LNG.let { LatLng(it.latitude + 0.1, it.longitude) },
                        fraction
                    )
                )
                fraction += 0.01
                if (fraction > 1) {
                    fraction = 0.0
                }
            }

        }
    }

    fun generateForPolyline(
        polyline: List<LatLng>,
        delay: Int = 1000,
        callback: (LatLng) -> Unit
    ): Job {
        return GlobalScope.launch {
            var last: LatLng? = null
            for (point in polyline) {
                if (last != null && SphericalUtil.computeDistanceBetween(last, point) < 100) {
                    continue
                }
                delay(delay.toLong())
                last = point
                callback.invoke(point)
            }
        }
    }

}
