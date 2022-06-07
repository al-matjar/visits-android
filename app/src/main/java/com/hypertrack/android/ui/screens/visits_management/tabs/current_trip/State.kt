package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.ReducerResult

sealed class State {
    override fun toString(): String = javaClass.simpleName
}

data class NotInitializedState(
    val userLocation: LatLng?,

    ) : State()

data class InitializedState(
    val map: HypertrackMapWrapper?,
    val trackingState: IsTrackingState
) : State()

fun State.withEffects(effects: Set<Effect>): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effects
    )
}

fun State.withEffects(vararg effect: Effect): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}
