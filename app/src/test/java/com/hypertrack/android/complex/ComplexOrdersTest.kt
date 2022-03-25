package com.hypertrack.android.complex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.interactors.TripInteractorTest
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.view_models.OrdersDetailsViewModelTest
import io.mockk.*
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@Suppress("EXPERIMENTAL_API_USAGE")
class ComplexOrdersTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should snooze an order on snooze click`() {
        val apiClient = TripInteractorTest.createMockApiClient(
            backendTrips = listOf(
                createBaseTrip(id = "1").copy(
                    orders = listOf(
                        createBaseOrder(id = "1", status = OrderStatus.ONGOING),
                        createBaseOrder(id = "2", status = OrderStatus.SNOOZED),
                    )
                )
            ),
            additionalConfig = {
                coEvery { it.snoozeOrder(any(), any()) } returns JustSuccess
                coEvery { it.unsnoozeOrder(any(), any()) } returns JustSuccess
            }
        )
        val tripsInteractor = TripInteractorTest.createTripInteractorImpl(
            apiClient = apiClient
        )
        val detailsVm1 = OrdersDetailsViewModelTest.createVm(
            id = "1",
            tripsInteractor,
            ordersInteractor = tripsInteractor
        )
        val detailsVm2 = OrdersDetailsViewModelTest.createVm(
            id = "2",
            tripsInteractor,
            ordersInteractor = tripsInteractor
        )

        runBlocking {
            tripsInteractor.refreshTrips()
            assertNotNull(tripsInteractor.getOrder("1"))

            detailsVm1.onSnoozeClicked()
            detailsVm2.onUnSnoozeClicked()

            coVerify {
                apiClient.snoozeOrder("1", "1")
                apiClient.unsnoozeOrder("2", "1")
            }
        }
    }

}
