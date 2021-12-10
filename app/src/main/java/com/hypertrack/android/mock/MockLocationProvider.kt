package com.hypertrack.android.mock

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.utils.DeviceLocationProvider
import kotlinx.coroutines.Job

class MockLocationProvider : DeviceLocationProvider {

    private val locationGenerator = LocationGenerator()
    private var generatorJob: Job? = null

    override
    val deviceLocation: MutableLiveData<LatLng?> = MutableLiveData()

    fun startGeneratingPolyline(polyline: List<LatLng>) {
        generatorJob?.cancel()
        generatorJob = locationGenerator.generateForPolyline(polyline, delay = 200) {
            deviceLocation.postValue(it)
        }
    }

    override fun getCurrentLocation(callback: (latLng: LatLng?) -> Unit) {
        callback.invoke(deviceLocation.value)
    }
}
