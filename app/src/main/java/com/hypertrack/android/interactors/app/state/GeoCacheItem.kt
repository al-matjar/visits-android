package com.hypertrack.android.interactors.app.state

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.MyApplication

data class GeoCacheItem(
    val geoHash: GeoHash,
    val status: GeoCacheItemStatus
)

sealed class GeoCacheItemStatus {
    override fun toString(): String = javaClass.simpleName
}

data class Loaded(
    val geofences: List<Geofence>
) : GeoCacheItemStatus() {
    override fun toString(): String {
        return if (true/*MyApplication.DEBUG_MODE*/) {
            "${javaClass.simpleName}(geofences=${geofences.size})"
        } else {
            super.toString()
        }
    }
}

data class LoadingError(
    val exception: Exception,
    val geofences: List<Geofence>,
    // null = first page
    val nextPageToken: String?,
) : GeoCacheItemStatus() {
    override fun toString(): String {
        return if (true/*MyApplication.DEBUG_MODE*/) {
            "${javaClass.simpleName}(geofences=${geofences.size}, nextPageToken=$nextPageToken, exception=$exception)"
        } else {
            super.toString()
        }
    }
}

object Loading : GeoCacheItemStatus()

