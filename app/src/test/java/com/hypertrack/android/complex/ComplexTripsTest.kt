package com.hypertrack.android.complex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.interactors.TripInteractorTest
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import com.hypertrack.android.view_models.OrdersDetailsViewModelTest
import io.mockk.*
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class ComplexTripsTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should update order completion state on orders list screen after changing it on order details screen`() {
        val tripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(
                createBaseTrip().copy(
                    orders = listOf(
                        createBaseOrder().copy(id = "1"),
                        createBaseOrder().copy(id = "2"),
                    )
                )
            ),
        )
        val listVm = OrdersListViewModel(
            mockk(relaxed = true),
            tripsInteractor,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
        val detailsVm1 = OrdersDetailsViewModelTest.createVm(
            id = "1",
            tripsInteractor
        )
        val detailsVm2 = OrdersDetailsViewModelTest.createVm(
            id = "2",
            tripsInteractor
        )

        runBlocking {
            tripsInteractor.refreshTrips()
            assertNotNull(tripsInteractor.getOrder("1"))

            val captured = mutableListOf<List<Order>>()
            listVm.orders.observeForever {
                captured.add(it)
            }

            detailsVm1.onCompleteClicked()
            captured.removeLast()
            captured.last().first { it.id == "1" }.let {
                assertEquals(OrderStatus.COMPLETED, it.status)
            }

            detailsVm2.onCancelClicked()
            captured.removeLast()
            captured.last().first { it.id == "2" }.let {
                assertEquals(OrderStatus.CANCELED, it.status)
            }
        }
    }

}
