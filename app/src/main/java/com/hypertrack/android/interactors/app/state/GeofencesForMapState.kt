package com.hypertrack.android.interactors.app.state

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.utils.MyApplication

data class GeofencesForMapState(
    val tiles: Map<GeoHash, GeoCacheItem> = mapOf(),
) {
    fun allGeofences(): List<Geofence> {
        return tiles.allGeofences()
    }

    val isLoadingForLocation by lazy {
        tiles.values.any { it.status is Loading }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(notLoadedTiles=${tiles.filter { it.value.status !is Loaded }})"
    }

    companion object {
        // geohash size, 1 - biggest
        const val GEOHASH_LETTERS_NUMBER = 4
    }
}

fun Map<GeoHash, GeoCacheItem?>.allGeofences(): List<Geofence> {
    return values.map {
        when (val status = it?.status) {
            is Loaded -> status.geofences
            is LoadingError -> status.geofences
            is Loading, null -> listOf()
        }
    }.flatten()
}
