package com.hypertrack.android.utils

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.screens.visits_management.tabs.places.Visit
import com.hypertrack.sdk.GeotagResult
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.OutageReason
import com.hypertrack.sdk.Result
import com.hypertrack.sdk.TrackingError
import com.hypertrack.sdk.TrackingStateObserver

class HyperTrackService(
    private val listener: TrackingState,
    private val sdkInstance: HyperTrack,
    private val crashReportsProvider: CrashReportsProvider? = null
) {

    init {
        when (sdkInstance.isRunning) {
            true -> listener.onTrackingStart()
            else -> listener.onTrackingStop()
        }
    }

    val deviceId: String
        get() {
            return if (!MyApplication.MOCK_MODE) {
                sdkInstance.deviceID
            } else {
                sdkInstance.deviceID
            }
        }

    val state: LiveData<TrackingStateValue>
        get() = listener.state

    val isTracking: LiveData<Boolean> = Transformations.map(state) {
        it == TrackingStateValue.TRACKING
    }

    val latestLocation: Result<Location, OutageReason>
        get() {
            return sdkInstance.latestLocation
        }

    fun setDeviceInfo(
        name: String?,
        email: String? = null,
        phoneNumber: String? = null,
        driverId: String? = null,
        deeplinkWithoutGetParams: String? = null,
        metadata: Map<String, Any>? = null
    ) {
        sdkInstance.setDeviceName(name)
        sdkInstance.setDeviceMetadata(mutableMapOf<String, Any>().apply {
            driverId.nullIfBlank()?.let { put(KEY_DRIVER_ID, it) }
            email.nullIfBlank()?.let { put(KEY_EMAIL, it) }
            phoneNumber.nullIfBlank()?.let { put(KEY_PHONE, it) }
            deeplinkWithoutGetParams.nullIfBlank()?.let { put(KEY_DEEPLINK, it) }
            metadata?.let { putAll(it) }
        }.apply {
            if (MyApplication.DEBUG_MODE) {
//                Log.v("hypertrack-verbose", "set device name: ${name}")
//                Log.v("hypertrack-verbose", "set device metadata: $this")
            }
        })
    }

    fun sendCompletionEvent(legacyOrder: LocalOrder, canceled: Boolean) {
        val payload = mapOf(
            "trip_id" to legacyOrder.id,
            "type" to if (!canceled) Constants.VISIT_MARKED_COMPLETE else Constants.VISIT_MARKED_CANCELED,
            LocalOrder.VISIT_NOTE_KEY to legacyOrder.note,
            LocalOrder.VISIT_PHOTOS_KEY to legacyOrder.photos.map { it.photoId }.toSet()
        )
        sdkInstance.addGeotag(payload, with(legacyOrder.destinationLatLng) {
            latitude.let {
                longitude.let {
                    val location = Location("visit")
                    location.longitude = longitude
                    location.latitude = latitude
                    location
                }
            }
        })
        crashReportsProvider?.log("sendCompletionEvent ${legacyOrder.id}")
    }

    fun sendPickedUp(id: String, typeKey: String) {
        crashReportsProvider?.log("sendPickedUp ${id}")
        sdkInstance.addGeotag(mapOf(typeKey to id, "type" to Constants.PICK_UP))
    }

    fun startTracking() {
        crashReportsProvider?.log("clockIn")
        sdkInstance.start()
    }

    fun stopTracking() {
        crashReportsProvider?.log("clockOut")
        sdkInstance.stop()
    }

    fun syncDeviceSettings() {
        crashReportsProvider?.log("syncDeviceSettings")
        sdkInstance.syncDeviceSettings()
    }

    fun showPermissionsPrompt() {
        sdkInstance.backgroundTrackingRequirement(false).requestPermissionsIfNecessary()
    }

    fun createGeotag(metadata: Map<String, String>): GeotagResult {
        return sdkInstance.addGeotag(metadata)
    }

    companion object {
        const val KEY_PHONE = "phone_number"
        const val KEY_EMAIL = "email"
        const val KEY_DEEPLINK = "invite_id"
        const val KEY_DRIVER_ID = "driver_id"
    }

}


class TrackingState(val crashReportsProvider: CrashReportsProvider? = null) :
    TrackingStateObserver.OnTrackingStateChangeListener {
    var state: MutableLiveData<TrackingStateValue> =
        MutableLiveData(TrackingStateValue.UNKNOWN)

    override fun onTrackingStart() {
        crashReportsProvider?.log("onTrackingStart")
        state.postValue(TrackingStateValue.TRACKING)
    }

    override fun onError(p0: TrackingError?) {
        crashReportsProvider?.log("TrackingError ${p0?.code} ${p0?.message}")
        when {
            p0?.code == TrackingError.AUTHORIZATION_ERROR && p0.message.contains("trial ended") -> state.postValue(
                TrackingStateValue.DEVICE_DELETED
            )
            p0?.code == TrackingError.PERMISSION_DENIED_ERROR -> {
                TrackingStateValue.PERMISIONS_DENIED
            }
            else -> state.postValue(TrackingStateValue.ERROR)
        }

    }

    override fun onTrackingStop() = state.postValue(TrackingStateValue.STOP)

    companion object {
        const val TAG = "HyperTrackService"
    }
}

enum class TrackingStateValue { TRACKING, ERROR, STOP, UNKNOWN, DEVICE_DELETED, PERMISIONS_DENIED }


