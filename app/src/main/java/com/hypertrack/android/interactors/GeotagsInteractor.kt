package com.hypertrack.android.interactors

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.sdk.GeotagResult
import com.hypertrack.sdk.OutageReason

class GeotagsInteractor(
    private val hyperTrackService: HyperTrackService
) {
    fun getLatestLocation(): LatestLocationResult {
        return try {
            return hyperTrackService.latestLocation.let {
                if (it.isSuccess) {
                    LatestLocation(it.value.toLatLng())
                } else {
                    Outage(it.error)
                }
            }
        } catch (e: Exception) {
            Error(e)
        }
    }

    fun createGeotag(metadata: Map<String, String>): GeotagCreationResult {
        return hyperTrackService.createGeotag(metadata).let {
            when (it) {
                is GeotagResult.Success, is GeotagResult.SuccessWithDeviation -> GeotagCreationSuccess
                is GeotagResult.Error -> GeotagCreationError(it.reason)
                else -> GeotagCreationException(IllegalArgumentException(it.toString()))
            }
        }
    }
}

sealed class LatestLocationResult {
    override fun toString(): String = javaClass.simpleName
}

data class LatestLocation(val latLng: LatLng) : LatestLocationResult()
data class Outage(val reason: OutageReason) : LatestLocationResult()
data class Error(val exception: Exception) : LatestLocationResult()

sealed class GeotagCreationResult {
    override fun toString(): String = javaClass.simpleName
}

object GeotagCreationSuccess : GeotagCreationResult()
data class GeotagCreationError(val reason: GeotagResult.Error.Reason) : GeotagCreationResult()
data class GeotagCreationException(val exception: Exception) : GeotagCreationResult()
