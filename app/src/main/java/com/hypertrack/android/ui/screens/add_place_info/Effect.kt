package com.hypertrack.android.ui.screens.add_place_info

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiEffect
import com.hypertrack.android.ui.common.map_state.UpdateGeofenceForDetailsEffect
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError
import com.hypertrack.android.use_case.app.UserScopeUseCases
import java.lang.Exception

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class InitEffect(val map: HypertrackMapWrapper) : Effect()

data class UpdateViewStateEffect(val state: State) : Effect()

// todo group gf params into one entity
data class ProceedWithAdjacentGeofenceCheckEffect(
    val params: GeofenceCreationParams,
    val radius: Int,
    val useCases: UserScopeUseCases
) : Effect()

data class CreateGeofenceEffect(
    val geofenceCreationData: GeofenceCreationData
) : Effect()

// use OnErrorAction on generic error (to change the state to Error)
data class ShowErrorMessageEffect(val error: DisplayableError) : Effect()
data class LogExceptionToCrashlyticsEffect(val exception: Exception) : Effect()
object OpenAddIntegrationScreenEffect : Effect()
data class MapUiEffect(val effect: MapUiEffect) : Effect()
data class UpdateRadiusAndZoomEffect(
    val location: LatLng,
    val radius: Int?,
    val updateRadiusEffect: UpdateGeofenceForDetailsEffect
) : Effect()

data class StartUpdateRadiusEffect(
    val location: LatLng,
    val radius: Int?,
) : Effect()
