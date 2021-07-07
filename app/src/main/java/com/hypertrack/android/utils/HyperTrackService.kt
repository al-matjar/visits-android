package com.hypertrack.android.utils

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.models.Visit
import com.hypertrack.android.models.VisitStatus
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.isEmail
import com.hypertrack.sdk.HyperTrack
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

    var driverId: String
        get() = throw NotImplementedError()
        set(value) {
            sdkInstance.setDeviceMetadata(mapOf("driver_id" to value))
            sdkInstance.setDeviceName(
                if (value.isEmail()) {
                    value.split("@")[0]
                } else {
                    value
                }
            )
        }

    val deviceId: String
        get() = sdkInstance.deviceID

    val state: LiveData<TrackingStateValue>
        get() = listener.state

    val isTracking: LiveData<Boolean> = Transformations.map(state) {
        it == TrackingStateValue.TRACKING
    }

    fun sendCompletionEvent(visit: Visit) {
        val payload = mapOf(
            visit.typeKey to visit._id,
            "type" to if (visit.state == VisitStatus.COMPLETED) Constants.VISIT_MARKED_COMPLETE else Constants.VISIT_MARKED_CANCELED,
            "visit_note" to visit.visitNote,
            "_visit_photos" to visit.photos.map { it.imageId }.toSet()
        )
        sdkInstance.addGeotag(payload, visit.expectedLocation)
        crashReportsProvider?.log("sendCompletionEvent ${visit._id}")
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

    fun createVisitStartEvent(id: String, typeKey: String) {
        sdkInstance.addGeotag(mapOf(typeKey to id, "type" to Constants.VISIT_ADDED))
    }

    fun sendPickedUp(id: String, typeKey: String) {
        crashReportsProvider?.log("sendPickedUp ${id}")
        sdkInstance.addGeotag(mapOf(typeKey to id, "type" to Constants.PICK_UP))
    }

    fun clockOut() {
        crashReportsProvider?.log("clockOut")
        sdkInstance.stop()
    }

    fun clockIn() {
        crashReportsProvider?.log("clockIn")
        sdkInstance.start()
    }

    fun syncDeviceSettings() {
        crashReportsProvider?.log("syncDeviceSettings")
        sdkInstance.syncDeviceSettings()
    }

    fun showPermissionsPrompt() {
        sdkInstance.backgroundTrackingRequirement(false).requestPermissionsIfNecessary()
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


