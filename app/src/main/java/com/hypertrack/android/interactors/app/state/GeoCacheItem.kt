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
        return "${javaClass.simpleName}(geofences=${geofences.size})"
    }
}

data class LoadingError(
    val exception: Exception,
    val geofences: List<Geofence>,
    // null = first page
    val nextPageToken: String?,
) : GeoCacheItemStatus() {
    override fun toString(): String {
        return listOf(
            "geofences=${geofences.size}",
            "nextPageToken=$nextPageToken",
            "exception=$exception"
        ).joinToString(", ").let {
            "${javaClass.simpleName}($it)"
        }
    }
}

data class Loading(
    val geofences: List<Geofence> = listOf(),
    // null = first page
    val nextPageToken: String? = null,
) : GeoCacheItemStatus()

