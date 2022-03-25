package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class InitEffect(val map: HypertrackMapWrapper) : Effect()

data class UpdateViewStateEffect(val viewState: ViewState) : Effect()
data class DisplayRadiusEffect(val map: HypertrackMapWrapper, val radius: Int?) : Effect()

// todo group gf params into one entity
data class ProceedWithAdjacentGeofenceCheckEffect(
    val params: GeofenceCreationParams,
    val radius: Int,
) : Effect()

data class CreateGeofenceEffect(
    val geofenceCreationData: GeofenceCreationData
) : Effect()

data class ShowErrorMessageEffect(val text: String) : Effect()
object OpenAddIntegrationScreenEffect : Effect()
