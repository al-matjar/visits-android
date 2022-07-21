package com.hypertrack.android.use_case.geofences

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.repository.PlacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("OPT_IN_USAGE")
class LoadGeofencesForMapUseCase(
    private val placesRepository: PlacesRepository
) {

    fun execute(geoHash: GeoHash, initialPageToken: String?): Flow<GeofencesPageResult> {
        return suspend {
            val result = mutableListOf<Geofence>()
            var pageToken = initialPageToken
            try {
                do {
                    val res = placesRepository.loadGeofencesPageForMap(geoHash, pageToken)
                    pageToken = res.paginationToken
                    result.addAll(res.items)
                } while (pageToken != null)
                PageSuccess(result)
            } catch (e: Exception) {
                PageFailure(e, result, pageToken)
            }
        }.asFlow()
    }

}

