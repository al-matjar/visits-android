package com.hypertrack.android.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.UserLocationChangedAction
import com.hypertrack.android.ui.common.util.toLatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface DeviceLocationProvider {
    val deviceLocation: MutableLiveData<LatLng?>
    fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit)
}

class FusedDeviceLocationProvider(
    private val appInteractor: AppInteractor,
    private val appCoroutineScope: CoroutineScope,
    private val context: Context,
    private val hyperTrackService: HyperTrackService,
    private val crashReportsProvider: CrashReportsProvider,
) : DeviceLocationProvider, LocationCallback() {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override val deviceLocation: MutableLiveData<LatLng?> = MutableLiveData()

    init {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationClient.requestLocationUpdates(LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_NO_POWER
                }, this, context.mainLooper)
            } else {
                crashReportsProvider.logException(Exception("No Location permissions"))
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    // todo remove legacy
    override fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit) {
        appCoroutineScope.launch {
            getCurrentLocationFromFusedProvider().let {
                it ?: hyperTrackService.latestLocation.toLatLng()
            }.also {
                withContext(Dispatchers.Main) {
                    callback.invoke(it)
                }
            }
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    private suspend fun getCurrentLocationFromFusedProvider(): LatLng? {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // lastLocation can be null
            getLastLocation(locationClient.lastLocation).let {
                when (it) {
                    is Success -> {
                        it.data?.toLatLng()
                    }
                    is Failure -> {
                        crashReportsProvider.logException(it.exception)
                        null
                    }
                }
            }
        } else {
            crashReportsProvider.logException(
                IllegalStateException(
                    "FusedDeviceLocationProvider: No location permissions"
                )
            )
            null
        }
    }

    private suspend fun getLastLocation(lastLocationTask: Task<Location?>): Result<Location?> =
        suspendCoroutine { continuation ->
            lastLocationTask.addOnSuccessListener {
                continuation.resume(it.asSuccess())
            }
            lastLocationTask.addOnFailureListener {
                continuation.resume(it.asFailure())
            }
        }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun onLocationResult(result: LocationResult) {
        // lastLocation can be null
        result.lastLocation?.toLatLng().let {
            appInteractor.handleAction(UserLocationChangedAction(it))
            deviceLocation.postValue(it)
        }
    }
}
