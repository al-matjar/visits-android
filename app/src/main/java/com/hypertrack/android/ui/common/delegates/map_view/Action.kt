package com.hypertrack.android.ui.common.delegates.map_view

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

sealed class Action
data class MapReadyAction(val map: GoogleMap) : Action()
data class MapViewCreatedAction(val mapView: MapView?) : Action()
object AttachedAction : Action()
object DetachedAction : Action()
object OnResumeAction : Action()
object OnPauseAction : Action()
object OnDestroyAction : Action()
object OnLowMemoryAction : Action()
