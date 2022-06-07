package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.Order
import com.hypertrack.logistics.android.github.R

data class TripData(
    val nextOrder: OrderData?,
    val ongoingOrders: List<Order>,
    val ongoingOrderText: String
)
