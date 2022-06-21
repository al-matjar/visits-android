package com.hypertrack.android.interactors.app.state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.sdk.NewTrackingState

sealed class UserState
object UserNotLoggedIn : UserState() {
    override fun toString(): String = javaClass.simpleName
}

data class UserLoggedIn(
    val deviceId: DeviceId,
    val userData: UserData,
    val userScope: UserScope,
    val trackingState: NewTrackingState,
    val userLocation: LatLng?,
    val history: HistoryState,
    val tripCreationScope: TripCreationScope? = null
) : UserState()
