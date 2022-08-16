package com.hypertrack.android.models.local

import com.hypertrack.android.api.Views
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Trip(
    val id: String,
    val status: TripStatus,
    val metadata: Map<String, String>,
    var orders: MutableList<Order>,
    val views: Views? = null
) {

    val nextOrder: Order?
        get() = ongoingOrders.firstOrNull()

    val ongoingOrders: List<Order>
        get() = orders.filter { it.status == OrderStatus.ONGOING }

    fun getOrder(orderId: String): Order? {
        return orders.firstOrNull { it.id == orderId }
    }

}
