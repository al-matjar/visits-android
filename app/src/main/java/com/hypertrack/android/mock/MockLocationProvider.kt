package com.hypertrack.android.mock

import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.mock.trips.MockTripStorage
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.Injector
import kotlinx.android.synthetic.main.activity_main.view.b1
import kotlinx.android.synthetic.main.activity_main.view.b2
import kotlinx.android.synthetic.main.activity_main.view.b3
import kotlinx.android.synthetic.main.activity_main.view.b4
import kotlinx.android.synthetic.main.activity_main.view.b5
import kotlinx.android.synthetic.main.activity_main.view.bStart
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

    fun bindButtons(lRecordingModeControls: ViewGroup) {
        lRecordingModeControls.bStart.setOnClickListener {
            Injector.mockLocationProvider.startGeneratingPolyline(
                MockData.MOCK_TRIP.orders.first().routeToPolyline!!
            )
        }
        lRecordingModeControls.b1.setOnClickListener {
            Injector.mockTripStorage.updateTrip(
                MockTripStorage.tripS_1_2
            )
        }
        lRecordingModeControls.b2.setOnClickListener {
            Injector.mockTripStorage.updateTrip(
                MockTripStorage.trip1_2
            )
        }
        lRecordingModeControls.b3.setOnClickListener {
            Injector.mockTripStorage.updateTrip(
                MockTripStorage.trip2_3
            )
        }
        lRecordingModeControls.b4.setOnClickListener {
            Injector.mockTripStorage.updateTrip(
                MockTripStorage.trip3
            )
        }
        lRecordingModeControls.b5.setOnClickListener { Injector.mockNotifications.sendTripNotification() }
    }
}
