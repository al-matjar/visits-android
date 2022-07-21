package com.hypertrack.android.hypertrack_sdk

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.sdk.OutageReason
import com.hypertrack.sdk.Result

sealed class LatestLocationResult {

    fun toLatLng(): LatLng? {
        return when (this) {
            is LatestLocation -> latLng
            is Outage -> null
        }
    }

    override fun toString(): String = javaClass.simpleName
}

data class LatestLocation(val latLng: LatLng) : LatestLocationResult()
data class Outage(val reason: OutageReason) : LatestLocationResult()
