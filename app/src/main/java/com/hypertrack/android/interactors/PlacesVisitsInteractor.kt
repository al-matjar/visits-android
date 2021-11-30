package com.hypertrack.android.interactors

import com.hypertrack.android.utils.Result

class PlacesVisitsInteractor(
    private val placesVisitsRepository: PlacesVisitsRepository
) {

    suspend fun getPlaceVisitsStats(): Result<PlaceVisitsStats> {
        return placesVisitsRepository.getPlaceVisitsStats()
    }

    fun invalidateCache() {
        placesVisitsRepository.invalidateCache()
    }
}