package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError
import java.lang.Exception

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class InitEffect(val map: HypertrackMapWrapper) : Effect()

data class UpdateViewStateEffect(val state: State) : Effect()
data class DisplayRadiusEffect(val map: HypertrackMapWrapper, val radius: Int?) : Effect()

// todo group gf params into one entity
data class ProceedWithAdjacentGeofenceCheckEffect(
    val params: GeofenceCreationParams,
    val radius: Int,
) : Effect()

data class CreateGeofenceEffect(
    val geofenceCreationData: GeofenceCreationData
) : Effect()

data class ShowErrorMessageEffect(val error: DisplayableError) : Effect()
data class LogExceptionToCrashlytics(val exception: Exception) : Effect()
object OpenAddIntegrationScreenEffect : Effect()
