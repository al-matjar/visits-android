package com.hypertrack.android.interactors

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.hypertrack_sdk.LatestLocationResult
import com.hypertrack.android.hypertrack_sdk.Outage
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.sdk.GeotagResult
import com.hypertrack.sdk.OutageReason

class GeotagsInteractor(
    private val hyperTrackService: HyperTrackService,
) {
    fun getLatestLocation(): LatestLocationResult {
        return hyperTrackService.latestLocation
    }

    fun createGeotag(metadata: Map<String, String>): Result<GeotagCreationResult> {
        return hyperTrackService.createGeotag(metadata).let {
            when (it) {
                is GeotagResult.Success, is GeotagResult.SuccessWithDeviation -> GeotagCreationSuccess.asSuccess()
                is GeotagResult.Error -> GeotagCreationError(it.reason).asSuccess()
                else -> IllegalArgumentException(it.toString()).asFailure()
            }
        }
    }
}

sealed class GeotagCreationResult {
    override fun toString(): String = javaClass.simpleName
}

object GeotagCreationSuccess : GeotagCreationResult()
data class GeotagCreationError(val reason: GeotagResult.Error.Reason) : GeotagCreationResult()
