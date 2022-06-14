package com.hypertrack.android.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Location
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.sdk.permissions.hasLocationPermission
import kotlin.Exception

interface DeviceLocationProvider {
    val deviceLocation: MutableLiveData<LatLng?>
    fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit)
}

class FusedDeviceLocationProvider(
    private val context: Context,
    private val crashReportsProvider: CrashReportsProvider
) : DeviceLocationProvider, LocationCallback() {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override val deviceLocation: MutableLiveData<LatLng?> = MutableLiveData()

    // todo check permissions
    init {
        try {
            locationClient.requestLocationUpdates(LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_NO_POWER
            }, this, context.mainLooper)
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    override fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                        crashReportsProvider.logException(it)
                        callback.invoke(null)
                    }
            } catch (e: Exception) {
                crashReportsProvider.logException(e)
                callback.invoke(null)
            }
        } else {
            crashReportsProvider.logException(IllegalStateException("FusedDeviceLocationProvider: No location permissions"))
            callback.invoke(null)
        }
    }

    override fun onLocationResult(result: LocationResult) {
        deviceLocation.postValue(result.lastLocation.toLatLng())
    }
}
