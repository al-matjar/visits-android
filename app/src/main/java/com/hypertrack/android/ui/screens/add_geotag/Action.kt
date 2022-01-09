package com.hypertrack.android.ui.screens.add_geotag

import com.hypertrack.android.interactors.GeotagCreationResult
import com.hypertrack.android.interactors.LatestLocationResult
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class Action
data class LatestLocationResultReceivedAction(val result: LatestLocationResult) : Action()
data class MapReadyAction(val map: HypertrackMapWrapper) : Action()
data class GeotagResultAction(val result: GeotagCreationResult) : Action()
data class CreateButtonClickAction(val metadata: Map<String, String>) : Action()
