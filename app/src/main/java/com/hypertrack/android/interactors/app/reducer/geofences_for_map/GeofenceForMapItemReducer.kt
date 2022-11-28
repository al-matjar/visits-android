package com.hypertrack.android.interactors.app.reducer.geofences_for_map

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.interactors.app.LoadGeofencesForMapEffect
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.Loading
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.withEffects

class GeofenceForMapItemReducer {

    // get loading state and load effect for item
    fun reduce(
        action: LoadGeofencesForMapAction,
        geoHash: GeoHash,
        itemState: GeoCacheItem?,
        useCases: UserScopeUseCases
    ): ReducerResult<out GeoCacheItem, out LoadGeofencesForMapEffect> {
        return when (val data = itemState?.status) {
            // no item (there wasn't loading attempts for this GeoHash before)
            null -> {
                GeoCacheItem(geoHash, Loading()).withEffects(
                    LoadGeofencesForMapEffect(geoHash, pageToken = null, useCases)
                )
            }
            is LoadingError -> {
                GeoCacheItem(geoHash, Loading(data.geofences, data.nextPageToken)).withEffects(
                    LoadGeofencesForMapEffect(geoHash, pageToken = data.nextPageToken, useCases)
                )
            }
            is Loading, is Loaded -> {
                // no need to init loading
                itemState.withEffects()
            }
        }
    }

}
