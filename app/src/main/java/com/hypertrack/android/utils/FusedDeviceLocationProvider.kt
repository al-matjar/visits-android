package com.hypertrack.android.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Location
import com.hypertrack.android.ui.common.util.toLatLng
import kotlin.Exception

interface DeviceLocationProvider {
    val deviceLocation: MutableLiveData<LatLng?>
    fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit)
}

@SuppressLint("MissingPermission")
class FusedDeviceLocationProvider(
    private val context: Context,
    private val crashReportsProvider: CrashReportsProvider
) : DeviceLocationProvider, LocationCallback() {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override val deviceLocation: MutableLiveData<LatLng?> = MutableLiveData()

    init {
        try {
            locationClient.requestLocationUpdates(LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_NO_POWER
            }, this, context.mainLooper)
//            getCurrentLocation {  }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit) {
        try {
            locationClient.lastLocation
                .addOnCompleteListener {
                    if (it.isSuccessful && it.result != null)
                        callback.invoke(it.result.toLatLng())
                    else {
                        callback.invoke(null)
                    }
                }
                .addOnFailureListener {
                    callback.invoke(null)
                }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    override fun onLocationResult(result: LocationResult) {
        deviceLocation.postValue(result.lastLocation.toLatLng())
    }
}
