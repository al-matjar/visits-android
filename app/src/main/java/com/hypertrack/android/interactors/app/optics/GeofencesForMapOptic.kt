package com.hypertrack.android.interactors.app.optics

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.GeofencesForMapState
import com.hypertrack.android.interactors.app.state.GeoCacheItem
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn

object GeofencesForMapOptic {

    fun get(state: AppState): GeofencesForMapState? {
        return when (state) {
            is AppInitialized -> when (state.userState) {
                is UserLoggedIn -> {
                    state.userState.geofencesForMap
                }
                UserNotLoggedIn -> null
            }
            is AppNotInitialized -> null
        }
    }

    fun set(
        state: AppInitialized,
        userState: UserLoggedIn,
        newGeofencesState: Map<GeoHash, GeoCacheItem>
    ): AppInitialized {
        return state.copy(
            userState = userState.copy(
                geofencesForMap = userState.geofencesForMap.copy(
                    tiles = newGeofencesState
                )
            )
        )
    }

}
