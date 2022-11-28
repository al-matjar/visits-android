package com.hypertrack.android.use_case.geofences

import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.GeoCacheItemStatus
import com.hypertrack.android.interactors.app.state.GeofencesForMapState
import com.hypertrack.android.interactors.app.state.GeofencesForMapStateTest.Companion.geofencesForMapState
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.Loading
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.mock.MockData.PALO_ALTO_LAT_LNG
import com.hypertrack.android.ui.common.util.getGeoHash
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.Success
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Test

@Suppress("OPT_IN_USAGE")
class CheckForAdjacentGeofencesUseCaseTest {

    @Test
    fun `it should wait for adjacent geofences to be loaded`() {
        val geoHash1 = PALO_ALTO_LAT_LNG.getGeoHash()
        val geoHash2 = geoHash1.adjacent[0]

        CheckForAdjacentGeofencesUseCase.shouldProceed(
            subState(
                mapOf()
            )
        ).let {
            assertEquals(true, it)
        }

        CheckForAdjacentGeofencesUseCase.shouldProceed(
            subState(
                mapOf(
                    geoHash1 to Loading(),
                    geoHash2 to Loading()
                )
            )
        ).let {
            assertEquals(false, it)
        }

        CheckForAdjacentGeofencesUseCase.shouldProceed(
            subState(
                mapOf(
                    geoHash1 to Loading(),
                    geoHash2 to Loaded(listOf())
                )
            )
        ).let {
            assertEquals(false, it)
        }

        CheckForAdjacentGeofencesUseCase.shouldProceed(
            subState(
                mapOf(
                    geoHash1 to Loaded(listOf()),
                    geoHash2 to Loaded(listOf())
                )
            )
        ).let {
            assertEquals(true, it)
        }

        CheckForAdjacentGeofencesUseCase.shouldProceed(
            subState(
                mapOf(
                    geoHash1 to Loading(),
                    geoHash2 to LoadingError(Exception(), listOf(), null)
                )
            )
        ).let {
            assertEquals(true, it)
        }
    }

    @Test
    fun `it should return correct result for geofences state`() {
        val geoHash1 = PALO_ALTO_LAT_LNG.getGeoHash()
        val geoHash2 = geoHash1.adjacent[0]
        val geoHash4 = geoHash1.adjacent[1]
        val intersect = Intersect()
        val checkIntersection = { _: Intersect, _: LatLng, _: Int, _: List<Loaded> -> false }

        CheckForAdjacentGeofencesUseCase.hasIntersection(
            checkIntersection,
            subState(
                mapOf(
                    geoHash4 to Loaded(listOf()),
                )
            ), useCachedOnly = false, intersect, PALO_ALTO_LAT_LNG, 500
        ).let {
            assertEquals(false, (it as Success).data)
        }

        val subState = subState(
            mapOf(
                geoHash4 to Loaded(listOf()),
                geoHash4 to Loading(),
            )
        )
        try {
            CheckForAdjacentGeofencesUseCase.hasIntersection(
                checkIntersection,
                subState,
                useCachedOnly = false,
                intersect,
                PALO_ALTO_LAT_LNG,
                500
            )
        } catch (e: Exception) {
            assertEquals(subState.toString(), e.message)
        }

        CheckForAdjacentGeofencesUseCase.hasIntersection(
            checkIntersection,
            subState(
                mapOf(
                    geoHash4 to Loaded(listOf()),
                    geoHash2 to LoadingError(Exception("1"), listOf(), null),
                )
            ), useCachedOnly = false, intersect, PALO_ALTO_LAT_LNG, 500
        ).let {
            assertEquals("1", (it as Failure).exception.message)
        }

        CheckForAdjacentGeofencesUseCase.hasIntersection(
            checkIntersection,
            subState(
                mapOf(
                    geoHash4 to Loaded(listOf()),
                    geoHash2 to LoadingError(Exception("1"), listOf(), null),
                )
            ), useCachedOnly = true, intersect, PALO_ALTO_LAT_LNG, 500
        ).let {
            assertEquals(false, (it as Success).data)
        }
    }

    companion object {
        fun subState(map: Map<GeoHash, GeoCacheItemStatus>): Map<GeoHash, GeoCacheItem> {
            return map.mapValues {
                GeoCacheItem(it.key, it.value)
            }
        }
    }

}
