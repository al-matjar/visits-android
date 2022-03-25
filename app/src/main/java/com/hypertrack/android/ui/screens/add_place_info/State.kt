package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.Optional
import com.hypertrack.android.utils.ReducerResult

sealed class State
object Initial : State()
data class Initialized(
    val map: HypertrackMapWrapper,
    val integrations: IntegrationsState,
    val address: String?,
    val radius: Int?
) : State()

data class CreatingGeofence(val previousState: Initialized) : State()

data class Error(
    val exception: Exception
) : State()

sealed class IntegrationsState {
    override fun toString(): String = javaClass.simpleName
}

data class IntegrationsEnabled(val integration: Integration?) : IntegrationsState()
data class IntegrationsDisabled(val geofenceName: String?) : IntegrationsState()

fun State.withEffects(effects: Set<Effect>): ReducerResult<State, Effect> {
    return ReducerResult(this, effects)
}

fun State.withEffects(vararg effect: Effect): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}


