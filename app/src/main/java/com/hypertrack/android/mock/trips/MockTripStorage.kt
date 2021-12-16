package com.hypertrack.android.mock.trips

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.mock.MockData
import com.hypertrack.android.models.Polyline
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R

class MockTripStorage() {

    val trip = MutableLiveData<LocalTrip?>()

    fun updateTrip(trip: LocalTrip?) {
        this.trip.postValue(trip)
    }

    fun updateOrder(orderId: String, updateFunction: (LocalOrder) -> LocalOrder) {
        trip.postValue(trip.value!!.apply {
            val index = orders.indexOfFirst { it.id == orderId }
            orders[index] = updateFunction(orders[index])
        })
    }

    companion object {
        private val mockOrders = MockData.MOCK_TRIP.orders.map { it.copy() }.let { source ->
            mutableListOf<LocalOrder>().also { res ->
                for (i in source.indices) {
                    if (i > 0) {
                        res.add(cutRoute(source[i], source[i - 1]))
                    } else {
                        res.add(source[i])
                    }
                }
            }
        }

        val tripS_1_2 = MockData.MOCK_TRIP.copy(
            orders = mutableListOf(
                mockOrders[0],
                mockOrders[1],
            )
        )

        val trip1_2 = MockData.MOCK_TRIP.copy(
            orders = mutableListOf(
                mockOrders[0].copy(status = OrderStatus.COMPLETED),
                mockOrders[1],
            )
        )

        val trip2_3 = MockData.MOCK_TRIP.copy(
            orders = mutableListOf(
                mockOrders[0].copy(status = OrderStatus.COMPLETED),
                mockOrders[1],
                mockOrders[2],
            )
        )

        val trip3 by lazy {
            MockData.MOCK_TRIP.copy(orders = mutableListOf(
                mockOrders[0].copy(status = OrderStatus.COMPLETED),
                mockOrders[1].copy(status = OrderStatus.SNOOZED),
                mockOrders[2].let {
                    it.copy(
                        estimate = it.estimate!!.copy(
                            route = path2_3.getPolylinePoints()
                        )
                    )
                }
            ))
        }

        val tripFinal by lazy {
            MockData.MOCK_TRIP.copy(orders = mutableListOf(
                mockOrders[0].copy(status = OrderStatus.COMPLETED),
                mockOrders[1].copy(status = OrderStatus.SNOOZED),
                mockOrders[2].let {
                    it.copy(
                        status = OrderStatus.COMPLETED,
                        estimate = it.estimate!!.copy(
                            route = path2_3.getPolylinePoints()
                        )
                    )
                }
            ))
        }

        private fun cutRoute(order: LocalOrder, previous: LocalOrder): LocalOrder {
            return order.copy(estimate = order.estimate?.copy(route = order.estimate.route?.let {
                Intersect().cutPathOnFirstRoughIntersection(
                    it,
                    previous.destinationLatLng
                )
            }))
        }

        private val PATH_2_to_3_JSON: String by lazy {
            MyApplication.context.resources.openRawResource(R.raw.mock_path_1_to_3).bufferedReader()
                .use { it.readText() }
        }

        private val path2_3: Polyline by lazy {
            Injector.getMoshi().adapter(Polyline::class.java).fromJson(PATH_2_to_3_JSON)!!
        }
    }
}


