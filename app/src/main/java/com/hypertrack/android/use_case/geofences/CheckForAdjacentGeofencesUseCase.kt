package com.hypertrack.android.use_case.geofences

import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.GeofencesForMapAppAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.reducer.GeofencesForMapReducer
import com.hypertrack.android.interactors.app.state.GeofencesForMapState
import com.hypertrack.android.interactors.app.state.GeofencesForMapState.Companion.GEOHASH_LETTERS_NUMBER
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.allGeofences
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.util.getGeoHash
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout

@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class CheckForAdjacentGeofencesUseCase(
    private val appInteractor: AppInteractor,
    private val geofencesForMapReducer: GeofencesForMapReducer,
    private val intersect: Intersect
) {

    fun execute(
        location: LatLng,
        radius: Int,
        stateFlow: StateFlow<GeofencesForMapState?>,
        useCachedOnly: Boolean = false
    ): Flow<Result<Boolean>> {
        return suspend {
            if (!useCachedOnly) {
                appInteractor.handleAction(
                    GeofencesForMapAppAction(
                        LoadGeofencesForMapAction(
                            location
                        )
                    )
                )
            }
            try {
                withTimeout(GEOFENCE_AREAS_LOADING_TIMEOUT) {
                    var subState: Map<GeoHash, GeoCacheItem?> = getTiles(stateFlow.value, location)
                    while (
                    // stop waiting if all loaded or if there is any error
                        !(subState.entries.any { it.value?.status is LoadingError }
                                || subState.entries.all { it.value?.status is Loaded }
                                || useCachedOnly)
                    ) {
                        stateFlow.collect()
                        subState = getTiles(stateFlow.value, location)
                    }
                    checkIntersection(
                        intersect,
                        location,
                        radius,
                        subState.allGeofences()
                    ).asSuccess()
                }
            } catch (e: TimeoutCancellationException) {
                AdjacentGeofencesCheckTimeoutException().asFailure()
            } catch (e: Exception) {
                e.asFailure()
            }
        }.asFlow()
    }

    private fun getTiles(
        state: GeofencesForMapState?,
        location: LatLng
    ): Map<GeoHash, GeoCacheItem?> {
        return state?.let {
            geofencesForMapReducer.getSubState(
                location.getGeoHash(GEOHASH_LETTERS_NUMBER),
                it.tiles
            )
        } ?: throw IllegalStateException("GeofencesForMapState is null")
    }

    companion object {
        const val GEOFENCE_AREAS_LOADING_TIMEOUT = 10000L

        private fun checkIntersection(
            intersect: Intersect,
            center: LatLng,
            radius: Int,
            allGeofences: List<Geofence>
        ): Boolean {
            val gh = center.getGeoHash(GEOHASH_LETTERS_NUMBER)
            allGeofences.filter {
                val checkGh = it.latLng.getGeoHash(GEOHASH_LETTERS_NUMBER)
                gh == checkGh || checkGh in gh.adjacent
            }.forEach {
                val intersects = if (it.isPolygon) {
                    intersect.areCircleAndPolygonIntersects(center, radius, it.polygon!!)
                } else {
                    intersect.areTwoCirclesIntersects(center, radius, it.latLng, it.radius)
                }
                if (intersects) return true
            }
            return false
        }
    }

}

