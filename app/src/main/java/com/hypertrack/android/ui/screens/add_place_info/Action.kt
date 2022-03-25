package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class Action {
    override fun toString(): String = javaClass.simpleName
}

class InitFinishedAction(
    val map: HypertrackMapWrapper,
    val hasIntegrations: Boolean,
    val address: String?,
    val geofenceName: String?
) : Action()

class MapReadyAction(val map: HypertrackMapWrapper) : Action()
class ErrorAction(val exception: Exception) : Action()
class GeofenceCreationErrorAction(val exception: Exception) : Action()
class IntegrationAddedAction(val integration: Integration) : Action()
class AddressChangedAction(val address: String) : Action()
class GeofenceNameChangedAction(val name: String) : Action()
class RadiusChangedAction(val radiusString: String) : Action()
object UpdateMapDataAction : Action()
object GeofenceNameClickedAction : Action()
object IntegrationDeletedAction : Action()
class ConfirmClickedAction(val params: GeofenceCreationParams) : Action()
class CreateGeofenceAction(val params: GeofenceCreationParams) : Action()

