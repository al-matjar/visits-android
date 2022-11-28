package com.hypertrack.android.interactors.app.reducer.geofences_for_map

import com.hypertrack.android.assertEffects
import com.hypertrack.android.assertNoEffects
import com.hypertrack.android.assertWithChecks
import com.hypertrack.android.createEffectCheck
import com.hypertrack.android.interactors.app.DeeplinkCheckTimeoutTimer
import com.hypertrack.android.interactors.app.LoadGeofencesForMapEffect
import com.hypertrack.android.interactors.app.StopTimer
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.GeoCacheItemStatus
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.Loading
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.mock.GeofenceMockData
import com.hypertrack.android.mock.MockData.PALO_ALTO_LAT_LNG
import com.hypertrack.android.ui.common.util.getGeoHash
import com.hypertrack.android.utils.state_machine.ReducerResult
import io.mockk.mockk
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.junit.Test

@Suppress("ComplexRedundantLet")
class GeofenceForMapItemReducerTest {

    @Test
    fun testAll() {
        val action = LoadGeofencesForMapAction(PALO_ALTO_LAT_LNG)
        val geoHash = PALO_ALTO_LAT_LNG.getGeoHash()
        val reducer = GeofenceForMapItemReducer()

        fun test(oldStatus: GeoCacheItemStatus?): ReducerResult<out GeoCacheItem, out LoadGeofencesForMapEffect> {
            return reducer.reduce(
                action,
                geoHash,
                oldStatus?.let { GeoCacheItem(geoHash, it) },
                useCases = mockk(),
            )
        }

        test(oldStatus = null).let {
            assertEquals(Loading(), it.newState.status)
            it.effects.assertEffects(
                LoadGeofencesForMapEffect::class
            )
        }

        val geofences = listOf(
            GeofenceMockData.createGeofence(PALO_ALTO_LAT_LNG)
        )
        val token = "token"
        test(oldStatus = LoadingError(Exception(), geofences, nextPageToken = token)).let {
            assertEquals(Loading(geofences, token), it.newState.status)
            it.effects.assertWithChecks(
                createEffectCheck<LoadGeofencesForMapEffect> { it.pageToken == token }
            )
        }

        test(oldStatus = Loaded(geofences)).let {
            assertEquals(Loaded(geofences), it.newState.status)
            it.effects.assertNoEffects()
        }

        test(oldStatus = Loading(geofences)).let {
            assertEquals(Loading(geofences), it.newState.status)
            it.effects.assertNoEffects()
        }
    }

}
