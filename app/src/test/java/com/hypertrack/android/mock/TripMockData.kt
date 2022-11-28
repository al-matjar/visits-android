package com.hypertrack.android.mock

import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.api.models.RemoteEstimate
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.api.models.RemoteTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.utils.datetime.toIso
import java.time.ZonedDateTime

object TripMockData {

    fun createTrip(
        id: String? = null,
        orders: List<RemoteOrder> = listOf(
            createOrder(status = OrderStatus.ONGOING),/*.copy(estimate=null)*/
            createOrder(status = OrderStatus.ONGOING),
            createOrder(status = OrderStatus.SNOOZED),
            createOrder(status = OrderStatus.CANCELED),
            createOrder(status = OrderStatus.COMPLETED),
        )
    ): RemoteTrip {
        return RemoteTrip(
            id ?: "tripId " + Math.random(),
            null,
            TripStatus.ACTIVE.value,
            ZonedDateTime.now().toIso(),
            mapOf(),
            null,
            RemoteEstimate(
                ZonedDateTime.now().toIso(),
                null
            ),
            orders,
        )
    }

    fun createOrder(
        id: String? = null,
        status: OrderStatus = OrderStatus.ONGOING
    ): RemoteOrder {
        return RemoteOrder(
            id ?: ("order " + Math.random()),
            TripDestination(MockData.PALO_ALTO_LAT_LNG, "Test Address"),
            status.value,
            ZonedDateTime.now().toIso(),
            ZonedDateTime.now().toIso(),
            RemoteEstimate(ZonedDateTime.now().plusMinutes(29).toIso(), null),
            mapOf(),
        )
    }
}
