package com.hypertrack.android.utils

import android.graphics.Point
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil

//todo optimize
class Intersect {

    fun areTwoCirclesIntersects(
        center1: LatLng,
        radius1: Int,
        center2: LatLng,
        radius2: Int
    ): Boolean {
        return SphericalUtil.computeDistanceBetween(center1, center2) <= radius1 + radius2
    }

    fun areCircleAndPolygonIntersects(center: LatLng, radius: Int, polygon: List<LatLng>): Boolean {
        if (polygonContains(center, polygon)) {
            return true
        }
        getPolygonEdgesInterpolation(polygon).forEach {
            if (circleContains(it, center, radius)) {
                return true
            }
        }
        return false
    }

    fun areTwoPolygonsIntersects(polygon1: List<LatLng>, polygon2: List<LatLng>): Boolean {
        if (polygonContains(polygon2.centroid(), polygon1)) {
            return true
        }
        getPolygonEdgesInterpolation(polygon1).forEach {
            if (polygonContains(it, polygon2)) {
                return true
            }
        }
        return false
    }

    fun cutPathOnFirstRoughIntersection(path: List<LatLng>, point: LatLng): List<LatLng> {
        for (i in path.indices) {
            if (SphericalUtil.computeDistanceBetween(path[i], point) < 100) {
                return path.subList(i, path.size)
            }
        }
        return path
    }

    private fun polygonContains(point: LatLng, polygon: List<LatLng>): Boolean {
        return PolyUtil.containsLocation(point, polygon, true)
    }

    private fun circleContains(point: LatLng, center: LatLng, radius: Int): Boolean {
        return SphericalUtil.computeDistanceBetween(point, center) <= radius
    }

    private fun getPolygonEdgesInterpolation(polygon: List<LatLng>): List<LatLng> {
        val res = PolyUtil.simplify(polygon, 25.0).toMutableList()
        for (i in 0 until polygon.size - 1) {
            val distance = SphericalUtil.computeDistanceBetween(
                polygon[i],
                polygon[i + 1]
            )
            var fraction = 0.0
            val step = 25 / distance
            while (fraction <= 1) {
                res.add(SphericalUtil.interpolate(polygon[i], polygon[i + 1], fraction))
                fraction += step
            }
        }
        return res
    }

}

private fun List<LatLng>.centroid(): LatLng {
    val lats = mutableListOf<Double>()
    val lons = mutableListOf<Double>()
    forEach {
        lats.add(it.latitude)
        lons.add(it.longitude)
    }
    return LatLng(lats.average(), lons.average())
}

private fun List<LatLng>.radius(): Double {
    val center = centroid()
    return map { SphericalUtil.computeDistanceBetween(center, it) }.maxOrNull()!!
}
