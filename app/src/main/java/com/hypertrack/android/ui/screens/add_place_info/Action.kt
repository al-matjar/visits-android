package com.hypertrack.android.ui.screens.add_place_info

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateGeofenceForDetailsMapUiAction
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

data class InitFinishedAction(
    val map: HypertrackMapWrapper,
    val hasIntegrations: Boolean,
    val location: LatLng,
    val address: String?,
    val geofenceName: String?
) : Action()

data class MapReadyAction(val map: HypertrackMapWrapper) : Action()
data class OnErrorAction(val error: DisplayableError) : Action()
data class GeofenceCreationErrorAction(val exception: Exception) : Action()
data class IntegrationAddedAction(val integration: Integration) : Action()
data class AddressChangedAction(val address: String) : Action()
data class GeofenceNameChangedAction(val name: String) : Action()
data class RadiusChangedAction(val radius: Int?) : Action()
object GeofenceNameClickedAction : Action()
object IntegrationDeletedAction : Action()
data class ConfirmClickedAction(val params: GeofenceCreationParams) : Action()
data class CreateGeofenceAction(val params: GeofenceCreationParams) : Action()
data class MapMovedAction(val position: LatLng) : Action()
data class MapUiAction(val action: MapUiAction) : Action()

