package com.hypertrack.android.use_case.geofences

import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.GeofencesForMapAppAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.reducer.geofences_for_map.GeofencesForMapReducer
import com.hypertrack.android.interactors.app.state.GeofencesForMapState
import com.hypertrack.android.interactors.app.state.GeofencesForMapState.Companion.GEOHASH_LETTERS_NUMBER
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.Loading
import com.hypertrack.android.ui.common.util.getGeoHash
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
                withTimeout(ADJACENT_GEOFENCE_CHECK_TIMEOUT) {
                    val subState: Map<GeoHash, GeoCacheItem?> = if (useCachedOnly) {
                        getTiles(stateFlow.value, location)
                    } else {
                        stateFlow.map { getTiles(it, location) }.first {
                            shouldProceed(it)
                        }
                    }
                    hasIntersection(
                        { intersect: Intersect, center: LatLng, radius: Int, tiles: List<Loaded> ->
                            checkIntersection(intersect, center, radius, tiles)
                        }, subState, useCachedOnly, intersect, location, radius
                    )
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
        const val ADJACENT_GEOFENCE_CHECK_TIMEOUT = 30000L

        fun shouldProceed(
            subState: Map<GeoHash, GeoCacheItem?>
        ): Boolean {
            // stop waiting if all loaded or if there is any error
            return (subState.entries.any { it.value?.status is LoadingError }
                    || subState.entries.all { it.value?.status is Loaded })
        }

        fun hasIntersection(
            checkIntersection: (
                intersect: Intersect,
                center: LatLng,
                radius: Int,
                tiles: List<Loaded>
            ) -> Boolean,
            subState: Map<GeoHash, GeoCacheItem?>,
            useCachedOnly: Boolean,
            intersect: Intersect,
            location: LatLng,
            radius: Int
        ): Result<Boolean> {
            val error = subState.entries.firstOrNull {
                it.value?.status is LoadingError
            }?.let {
                (it.value?.status as LoadingError).exception
            }
            return if (error != null && !useCachedOnly) {
                error.asFailure()
            } else {
                val loaded = subState.entries.mapNotNull {
                    when (val status = it.value?.status) {
                        is Loaded -> status
                        is LoadingError, null -> null
                        is Loading -> {
                            if (useCachedOnly) {
                                // in this case app doesn't wait for everything to be loaded
                                null
                            } else {
                                return IllegalArgumentException(subState.toString()).asFailure()
                            }
                        }
                    }
                }
                checkIntersection(
                    intersect,
                    location,
                    radius,
                    loaded
                ).asSuccess()
            }
        }

        private fun checkIntersection(
            intersect: Intersect,
            center: LatLng,
            radius: Int,
            tiles: List<Loaded>
        ): Boolean {
            val allGeofences = tiles.map { it.geofences }.flatten()
            val gh = center.getGeoHash(GEOHASH_LETTERS_NUMBER)
            allGeofences.filter {
                val checkGh = it.location.getGeoHash(GEOHASH_LETTERS_NUMBER)
                gh == checkGh || checkGh in gh.adjacent
            }.forEach {
                val intersects = if (it.isPolygon) {
                    intersect.areCircleAndPolygonIntersects(center, radius, it.polygon!!)
                } else {
                    intersect.areTwoCirclesIntersects(center, radius, it.location, it.radius)
                }
                if (intersects) return true
            }
            return false
        }
    }

}

