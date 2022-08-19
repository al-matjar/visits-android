package com.hypertrack.android.utils

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.hypertrack_sdk.LatestLocation
import com.hypertrack.android.hypertrack_sdk.LatestLocationResult
import com.hypertrack.android.hypertrack_sdk.Outage
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.sdk.GeotagResult
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.OutageReason
import com.hypertrack.sdk.TrackingError
import com.hypertrack.sdk.TrackingStateObserver

// todo move to com.hypertrack.android.hypertrack_sdk
class HyperTrackService(
    private val sdk: HyperTrack,
    private val crashReportsProvider: CrashReportsProvider
) {

    val isServiceRunning: Boolean
        get() {
            return sdk.isRunning
        }

    val deviceId: String
        get() {
            return sdk.deviceID
        }

    val latestLocation: LatestLocationResult
        get() {
            return sdk.latestLocation.let {
                if (it.isSuccess) {
                    LatestLocation(it.value.toLatLng())
                } else {
                    Outage(it.error)
                }
            }
        }

    fun setDeviceInfo(
        name: String?,
        email: String? = null,
        phoneNumber: String? = null,
        driverId: String? = null,
        deeplinkWithoutGetParams: String? = null,
        metadata: Map<String, Any>? = null
    ) {
        sdk.setDeviceName(name)
        sdk.setDeviceMetadata(mutableMapOf<String, Any>().apply {
            driverId.nullIfBlank()?.let { put(KEY_DRIVER_ID, it) }
            email.nullIfBlank()?.let { put(KEY_EMAIL, it) }
            phoneNumber.nullIfBlank()?.let { put(KEY_PHONE, it) }
            deeplinkWithoutGetParams.nullIfBlank()?.let { put(KEY_DEEPLINK, it) }
            metadata?.let { putAll(it) }
        }.apply {
            crashReportsProvider.log("set device name: $name")
            crashReportsProvider.log("set device metadata: $this")
        })
    }

    fun startTracking() {
        crashReportsProvider.log("clockIn")
        sdk.start()
    }

    fun stopTracking() {
        crashReportsProvider.log("clockOut")
        sdk.stop()
    }

    fun syncDeviceSettings() {
        crashReportsProvider.log("syncDeviceSettings")
        sdk.syncDeviceSettings()
    }

    fun showPermissionsPrompt() {
        crashReportsProvider.log("showPermissionsPrompt")
        sdk.backgroundTrackingRequirement(false).requestPermissionsIfNecessary()
    }

    fun createGeotag(metadata: Map<String, String>): Result<GeotagResult> {
        crashReportsProvider.log("createGeotag $metadata")
        return tryAsResult {
            sdk.addGeotag(metadata)
        }
    }

    companion object {
        const val KEY_PHONE = "phone_number"
        const val KEY_EMAIL = "email"
        const val KEY_DEEPLINK = "invite_id"
        const val KEY_DRIVER_ID = "driver_id"
    }

}

@Deprecated("refactor")
class TrackingState(val crashReportsProvider: CrashReportsProvider? = null) :
    TrackingStateObserver.OnTrackingStateChangeListener {
    val state: MutableLiveData<TrackingStateValue> =
        MutableLiveData()

    override fun onTrackingStart() {
        crashReportsProvider?.log("onTrackingStart")
        state.postValue(TrackingStateValue.TRACKING)
    }

    override fun onError(trackingError: TrackingError) {
        crashReportsProvider?.log("TrackingError: $trackingError")
        state.postValue(
            when {
                trackingError.code == TrackingError.AUTHORIZATION_ERROR
                        && trackingError.message.contains("trial ended") -> {
                    TrackingStateValue.DEVICE_DELETED
                }
                trackingError.code == TrackingError.PERMISSION_DENIED_ERROR -> {
                    TrackingStateValue.PERMISIONS_DENIED
                }
                else -> {
                    TrackingStateValue.ERROR
                }
            }
        )
    }

    override fun onTrackingStop() = state.postValue(TrackingStateValue.STOP)

    companion object {
        const val TAG = "HyperTrackService"
    }
}

enum class TrackingStateValue { TRACKING, ERROR, STOP, UNKNOWN, DEVICE_DELETED, PERMISIONS_DENIED }
