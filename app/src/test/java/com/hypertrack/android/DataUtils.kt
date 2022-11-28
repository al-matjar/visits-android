package com.hypertrack.android

import com.hypertrack.android.api.Point
import com.hypertrack.android.api.models.RemoteTrip
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.api.Views
import com.hypertrack.android.mock.MockData
import com.hypertrack.android.api.models.RemoteEstimate
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.utils.datetime.toIso
import java.time.ZonedDateTime

fun createBaseOrder(
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

//todo replace with MockData
fun createBaseTrip(id: String? = null): RemoteTrip {
    return RemoteTrip(
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

