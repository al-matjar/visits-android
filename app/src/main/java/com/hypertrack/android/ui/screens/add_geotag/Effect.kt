package com.hypertrack.android.ui.screens.add_geotag

import androidx.annotation.StringRes
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper

sealed class Effect {
    override fun toString(): String = javaClass.simpleName
}

data class UpdateViewStateEffect(val state: State) : Effect()
data class ShowMetadataErrorEffect(
    // Pair<key, value>
    val metadataItems: List<Pair<String, String>>,
    val metadataError: MetadataError
) : Effect()

object ShowMapNotReadyErrorEffect : Effect()
data class ShowToastEffect(@StringRes val stringResource: Int) : Effect()
data class CreateGeotag(val metadata: Map<String, String>) : Effect()
object GoBackEffect : Effect()
data class ShowOnMapEffect(val map: HypertrackMapWrapper, val latestLocation: LatLng) : Effect()
