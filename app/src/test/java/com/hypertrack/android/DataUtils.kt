package com.hypertrack.android

import com.hypertrack.android.api.Point
import com.hypertrack.android.api.Trip
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.api.Views
import com.hypertrack.android.models.RemoteEstimate
import com.hypertrack.android.models.Order
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.mock.MockData

//todo replace with MockData
fun createBaseOrder(
    id: String? = null,
    status: OrderStatus = OrderStatus.ONGOING
): Order {
    return MockData.createOrder(id, status)
}

//todo replace with MockData
fun createBaseTrip(id: String? = null): Trip {
    return Trip(
        id = id ?: "tripId " + Math.random(),
        status = TripStatus.ACTIVE.value,
        orders = null,
        views = Views("", null),
        createdAt = "",
        metadata = emptyMap(),
        destination = TripDestination(
            null,
            Point(listOf(42.0, 42.0)),
            0,
            arrivedAt = "2020-02-02T20:20:02.020Z"
        ),
        estimate = RemoteEstimate(
            "",
            null
        )
    )
}

