package com.hypertrack.android.view_models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.interactors.TripInteractorTest
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@Suppress("EXPERIMENTAL_API_USAGE")
class OrdersListViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should show ongoing orders first (with preserved backend order), then orders with other statuses`() {
        val backendOrders = listOf<RemoteOrder>(
            createBaseOrder().copy(
                id = "1",
                _status = OrderStatus.COMPLETED.value
            ),
            createBaseOrder().copy(
                id = "2",
                _status = OrderStatus.ONGOING.value
            ),
            createBaseOrder().copy(
                id = "3",
                _status = OrderStatus.CANCELED.value
            ),
            createBaseOrder().copy(
                id = "4",
                _status = OrderStatus.ONGOING.value
            ),
        )
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(createBaseTrip().copy(orders = backendOrders))
        )
        runBlocking {
            tripsInteractor.refreshTrips()
            assertTrue(tripsInteractor.currentTrip.value != null)
            val vm = OrdersListViewModel(
                mockk(relaxed = true),
                tripsInteractor,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
            vm.orders.observeForever {}
            val orders = vm.orders.value!!
//            orders.forEach { println("${it.id} ${it.status}") }
            assertEquals("2", orders[0].id)
            assertEquals("4", orders[1].id)
            assertEquals("1", orders[2].id)
            assertEquals("3", orders[3].id)
        }

    }

}
