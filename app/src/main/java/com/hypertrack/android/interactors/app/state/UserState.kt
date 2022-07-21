package com.hypertrack.android.interactors.app.state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.use_case.sdk.TrackingState

sealed class UserState
object UserNotLoggedIn : UserState() {
    override fun toString(): String = javaClass.simpleName
}

data class UserLoggedIn(
    val deviceId: DeviceId,
    val userScope: UserScope,
    val useCases: UserScopeUseCases,
    val userData: UserData,
    val trackingState: TrackingState,
    val history: HistoryState,
    val userLocation: LatLng?,
    val geofencesForMap: GeofencesForMapState = GeofencesForMapState(),
    val tripCreationScope: TripCreationScope? = null,
) : UserState()

