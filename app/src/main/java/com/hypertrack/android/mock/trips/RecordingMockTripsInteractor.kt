package com.hypertrack.android.mock.trips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.OrderCompletionResponse
import com.hypertrack.android.api.OrderCompletionSuccess
import com.hypertrack.android.interactors.AddOrderResult
import com.hypertrack.android.interactors.AddOrderSuccess
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.SimpleResult
import kotlinx.coroutines.delay

class RecordingMockTripsInteractor(
    tripsInteractor: TripsInteractor,
    private val mockTripStorage: MockTripStorage
) : TripsInteractor by tripsInteractor {

    override val currentTrip: LiveData<LocalTrip?> = mockTripStorage.trip

    override fun getOrderLiveData(orderId: String): LiveData<LocalOrder> {
        return Transformations.switchMap(currentTrip) {
            it?.orders?.firstOrNull { it.id == orderId }?.let { MutableLiveData(it) }
                ?: MutableLiveData()
        }
    }

    override suspend fun completeOrder(orderId: String): OrderCompletionResponse {
        delay(1000)
        mockTripStorage.updateOrder(orderId) {
            it.copy(status = OrderStatus.COMPLETED, completedAt = it.scheduledAt?.plusMinutes(10))
        }
        if (orderId == mockTripStorage.trip.value!!.orders.getOrNull(2)?.id) {
            mockTripStorage.updateTrip(MockTripStorage.tripFinal)
        }
        return OrderCompletionSuccess
    }

    override suspend fun snoozeOrder(orderId: String): SimpleResult {
        delay(1000)
        mockTripStorage.updateOrder(orderId) {
            it.copy(status = OrderStatus.SNOOZED)
        }
        mockTripStorage.updateTrip(MockTripStorage.trip3)
        return JustSuccess
    }

    override suspend fun addOrderToTrip(
        tripId: String,
        latLng: LatLng,
        address: String?
    ): AddOrderResult {
        delay(1000)
        mockTripStorage.updateTrip(MockTripStorage.trip2_3)
        return AddOrderSuccess
    }

    override suspend fun completeTrip(tripId: String): SimpleResult {
        delay(1000)
        mockTripStorage.updateTrip(null)
        return JustSuccess
    }
}
