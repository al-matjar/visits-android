package com.hypertrack.android.interactors.app.reducer

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.LoadGeofencesForMapEffect
import com.hypertrack.android.interactors.app.ShowAndReportAppErrorEffect
import com.hypertrack.android.interactors.app.action.ClearGeofencesForMapAction
import com.hypertrack.android.interactors.app.action.GeofencesForMapAction
import com.hypertrack.android.interactors.app.action.GeofencesForMapLoadedAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.state.GeoCacheItemStatus
import com.hypertrack.android.interactors.app.state.GeofencesForMapState.Companion.GEOHASH_LETTERS_NUMBER
import com.hypertrack.android.interactors.app.state.LoadingError
import com.hypertrack.android.interactors.app.state.Loaded
import com.hypertrack.android.interactors.app.state.Loading
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.util.getGeoHash
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.use_case.geofences.PageFailure
import com.hypertrack.android.use_case.geofences.PageSuccess
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.withEffects

class GeofencesForMapReducer {

    fun reduce(
        action: GeofencesForMapAction,
        state: Map<GeoHash, GeoCacheItem>,
        useCases: UserScopeUseCases
    ): ReducerResult<Map<GeoHash, GeoCacheItem>, out AppEffect> {
        return when (action) {
            is LoadGeofencesForMapAction -> {
                val geoHash = action.location.getGeoHash(GEOHASH_LETTERS_NUMBER)
                // load the target location GeoHash and 8 adjacent ones
                val subState = getSubState(geoHash, state)
                reduce(action, subState, useCases).withState { newSubState ->
                    state.toMutableMap().apply {
                        putAll(newSubState)
                    }
                }
            }
            is GeofencesForMapLoadedAction -> {
                val geoHash = action.geoHash
                state[geoHash]?.let { oldItem ->
                    // get new item state with loaded data
                    reduce(action, geoHash, oldItem, state)
                        .let { result ->
                            // put it to map state
                            state.toMutableMap().also {
                                it[geoHash] = oldItem.copy(status = result.newState)
                            }.withEffects(result.effects)
                        }
                } ?: state.withEffects(
                    ShowAndReportAppErrorEffect(IllegalActionException(action, state))
                )
            }
            ClearGeofencesForMapAction -> {
                mapOf<GeoHash, GeoCacheItem>().withEffects()
            }
        }
    }

    fun getSubState(
        geoHash: GeoHash,
        state: Map<GeoHash, GeoCacheItem>
    ): Map<GeoHash, GeoCacheItem?> {
        val adjacent = (listOf(geoHash) + geoHash.adjacent)
        val subState = mutableMapOf<GeoHash, GeoCacheItem?>().apply {
            adjacent.forEach { put(it, state[it]) }
        }
        return subState
    }

    // get loading state and load effect for each item in subState
    private fun reduce(
        action: LoadGeofencesForMapAction,
        subState: Map<GeoHash, GeoCacheItem?>,
        useCases: UserScopeUseCases
    ): ReducerResult<MutableMap<GeoHash, GeoCacheItem>, AppEffect> {
        val results = subState.map {
            // get the new state for each and loading effects if needed
            reduce(action, it.key, it.value, useCases)
        }
        val newState = mutableMapOf<GeoHash, GeoCacheItem>().apply {
            results.forEach { put(it.newState.geoHash, it.newState) }
        }
        val effects = results.map { it.effects }.flatten().toSet()
        return newState.withEffects(effects)
    }

    // get loading state and load effect for item
    private fun reduce(
        action: LoadGeofencesForMapAction,
        geoHash: GeoHash,
        itemState: GeoCacheItem?,
        useCases: UserScopeUseCases
    ): ReducerResult<GeoCacheItem, LoadGeofencesForMapEffect> {
        return when (val data = itemState?.status) {
            // no item (there wasn't loading attempts for this GeoHash before)
            null -> {
                GeoCacheItem(geoHash, Loading).withEffects(
                    LoadGeofencesForMapEffect(geoHash, pageToken = null, useCases)
                )
            }
            is LoadingError -> {
                itemState.withEffects(
                    LoadGeofencesForMapEffect(geoHash, pageToken = data.nextPageToken, useCases)
                )
            }
            is Loading, is Loaded -> {
                // no need to init loading
                itemState.withEffects()
            }
        }
    }

    // get new item state with loaded data
    private fun reduce(
        action: GeofencesForMapLoadedAction,
        geoHash: GeoHash,
        oldItem: GeoCacheItem,
        // used only for impossible state exception
        state: Map<GeoHash, GeoCacheItem>
    ): ReducerResult<GeoCacheItemStatus, AppEffect> {
        return when (val oldStatus = oldItem.status) {
            // if was loading before
            is Loading -> {
                when (action.result) {
                    is PageSuccess -> {
                        Loaded(action.result.geofences).withEffects()
                    }
                    // if batch failed
                    is PageFailure -> {
                        reduce(
                            action.result,
                            listOf()
                        )
                    }
                }
            }
            // if was failed before
            is LoadingError -> {
                when (action.result) {
                    is PageSuccess -> {
                        Loaded(oldStatus.geofences + action.result.geofences).withEffects()
                    }
                    // if batch failed
                    is PageFailure -> {
                        reduce(
                            action.result,
                            oldStatus.geofences,
                        )
                    }
                }
            }
            is Loaded -> {
                // impossible state
                // for some
                oldItem.status.withEffects(
                    ShowAndReportAppErrorEffect(IllegalActionException(action, state))
                )
            }
        }
    }

    private fun reduce(
        result: PageSuccess,
        geoHash: GeoHash,
        oldGeofences: List<Geofence>
    ): ReducerResult<GeoCacheItemStatus, AppEffect> {
        return Loaded(oldGeofences + result.geofences).withEffects()
    }

    private fun reduce(
        result: PageFailure,
        oldGeofences: List<Geofence>
    ): ReducerResult<GeoCacheItemStatus, AppEffect> {
        // don't retry (it will be triggered by next map move)
        return LoadingError(
            result.exception,
            oldGeofences + result.loaded,
            result.nextPageToken
        ).withEffects(
            ShowAndReportAppErrorEffect(result.exception)
        )
    }

}

