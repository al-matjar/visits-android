package com.hypertrack.android.interactors

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.MainCoroutineScopeRule
import com.hypertrack.android.api.*
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.api.models.RemoteTrip
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.interactors.trip.TripsInteractorImpl
import com.hypertrack.android.models.local.Trip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.repository.*
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.JustSuccess
import io.mockk.*
import io.mockk.coVerify
import junit.framework.Assert.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class TripInteractorTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    //todo test error emission
    @Test
    fun `it should load trips with orders`() {
        val backendTrips = listOf(
            createBaseTrip().copy(
                orders = listOf(
                    createBaseOrder(),
                    createBaseOrder()
                ),
                status = TripStatus.ACTIVE.value
            ),
            createBaseTrip().copy(
                orders = listOf(
                    createBaseOrder(),
                ),
                status = TripStatus.ACTIVE.value
            ),
            createBaseTrip().copy(
                orders = listOf(
                    createBaseOrder(),
                ),
                status = TripStatus.COMPLETED.value
            ),
            createBaseTrip().copy(
                orders = listOf(
                    createBaseOrder(),
                ),
                status = TripStatus.PROGRESSING_COMPLETION.value
            ),
        )
        val tripStorage = mockk<TripsStorage>() {
            coEvery { getTrips() } returns listOf()
            coEvery { saveTrips(any()) } returns kotlin.Unit
        }
        val tripsInteractor: TripsInteractor = createTripInteractorImpl(
            tripStorage, backendTrips
        )
        runBlocking {
            tripsInteractor.refreshTrips()
        }

        runBlocking {
            val completed = tripsInteractor.completedTrips.observeAndGetValue()
            assertEquals(2, completed.size)
            assertTrue(completed.all { it.orders.size == 1 })
            val slot = slot<List<Trip>>()
            coVerify {
                tripStorage.saveTrips(capture(slot))
            }
            println(slot.captured)
            assertEquals(4, slot.captured.size)
        }
    }

    @Test
    fun `it should get orders list for current trip (first with ongoing status)`() {
        val backendOrders = listOf(
            createBaseTrip().copy(
                id = "tripId",
                status = TripStatus.ACTIVE.value, orders = listOf<RemoteOrder>(
                    createBaseOrder().copy(
                        id = "1",
                        _status = OrderStatus.COMPLETED.value
                    ),
                    createBaseOrder().copy(
                        id = "2",
                        _status = OrderStatus.CANCELED.value
                    ),
                    createBaseOrder().copy(
                        id = "3",
                        _status = OrderStatus.ONGOING.value
                    ),
                )
            ),
            createBaseTrip().copy(
                id = "tripId1",
                status = TripStatus.ACTIVE.value, orders = listOf(
                    createBaseOrder().copy(
                        _status = OrderStatus.ONGOING.value
                    )
                )
            )
        )
        val tripsInteractor: TripsInteractor = createTripInteractorImpl(
            backendTrips = backendOrders
        )
        runBlocking {
            tripsInteractor.refreshTrips()
        }

        runBlocking {
            tripsInteractor.currentTrip.observeAndGetValue()!!.let {
                assertEquals("tripId", it.id)
                assertEquals(3, it.orders.size)
            }
        }
    }

    @Test
    fun `it should persist local orders state when refreshing trips`() {
        var trip =
            createBaseTrip().copy(
                orders = listOf(
                    createBaseOrder().copy(id = "1"),
                    createBaseOrder().copy(id = "2"),
                    createBaseOrder().copy(id = "3"),
                )
            )
        val tripsInteractorImpl = createTripInteractorImpl(
            tripStorage = createTripsStorage(),
            apiClient = mockk {
                coEvery { getTrips() } answers {
                    listOf(trip)
                }
                coEvery { completeOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { cancelOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { updateOrderMetadata(any(), any(), any()) } answers {
                    trip = trip.copy(orders = trip.orders!!.map {
                        if (it.id == firstArg()) {
                            val paramMd = thirdArg<Metadata>()
                            try {
                                val copyMd = it.copy(
                                    _metadata = paramMd.copy(visitsAppMetadata = paramMd.visitsAppMetadata.copy())
                                        .toMap()
                                )
                                return@map copyMd
                            } catch (e: Exception) {
                                e.printStackTrace()
                                throw e
                            }
                        } else {
                            it
                        }
                    })
                    Response.success(trip)
                }
            },
        )
        runBlocking {
            tripsInteractorImpl.refreshTrips()
            tripsInteractorImpl.addPhotoToOrder("2", "")
            tripsInteractorImpl.addPhotoToOrder("2", "")
            tripsInteractorImpl.addPhotoToOrder("3", "")
            tripsInteractorImpl.refreshTrips()
        }
        runBlocking {
            tripsInteractorImpl.currentTrip.observeAndGetValue()!!.let { trip ->
                trip.orders.let { orders ->
                    orders[0].let {
                        assertEquals(0, it.photos.size)
                    }
                    orders[1].let {
                        assertEquals(2, it.photos.size)
                    }
                    orders[2].let {
                        assertEquals(1, it.photos.size)
                    }
                }
            }
        }
    }

    @Test
    fun `it should complete order on order complete`() {
        val trip = createBaseTrip().copy(
            id = "3", status = TripStatus.ACTIVE.value, orders = listOf(
                createBaseOrder().copy(id = "1"),
                createBaseOrder().copy(id = "2"),
            )
        )
        val backendTrips = listOf(
            trip,
        )
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { getTrips() } returns backendTrips
            coEvery { completeTrip(any()) } returns JustSuccess
            coEvery { updateOrderMetadata(any(), any(), any()) } returns Response.success(trip)
        }
        val tripsInteractorImpl = createTripInteractorImpl(
            backendTrips = backendTrips,
            apiClient = apiClient
        )
        runBlocking {
            tripsInteractorImpl.refreshTrips()
            tripsInteractorImpl.completeOrder("1")
            coVerifyAll {
                apiClient.getTrips()
                apiClient.completeOrder("1", "3")
            }
        }
    }

    @Test
    fun `it should cancel order on order cancel`() {
        val trip = createBaseTrip().copy(
            id = "3", status = TripStatus.ACTIVE.value, orders = listOf(
                createBaseOrder().copy(id = "1"),
                createBaseOrder().copy(id = "2"),
            )
        )
        val backendTrips = listOf(
            trip,
        )
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { getTrips() } returns backendTrips
            coEvery { completeTrip(any()) } returns JustSuccess
            coEvery { updateOrderMetadata(any(), any(), any()) } returns Response.success(trip)
        }
        val tripsInteractorImpl = createTripInteractorImpl(
            backendTrips = backendTrips,
            apiClient = apiClient
        )
        runBlocking {
            tripsInteractorImpl.refreshTrips()
            tripsInteractorImpl.cancelOrder("1")
            coVerify {
                apiClient.getTrips()
                apiClient.cancelOrder("1", "3")
            }
        }
    }

    @Test
    fun `it should update metadata before order completion or cancellation if it was changed`() {
        val backendTrips = listOf(
            createBaseTrip().copy(
                id = "3", status = TripStatus.ACTIVE.value, orders = listOf(
                    createBaseOrder().copy(id = "1"),
                    createBaseOrder().copy(id = "2"),
                    createBaseOrder().copy(id = "3"),
                    createBaseOrder().copy(id = "4"),
                    createBaseOrder().copy(id = "5"),
                )
            ),
        )
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { getTrips() } returns backendTrips
            coEvery { completeTrip(any()) } returns JustSuccess
            coEvery { completeOrder(any(), any()) } returns OrderCompletionSuccess
            coEvery { cancelOrder(any(), any()) } returns OrderCompletionSuccess
            coEvery { updateOrderMetadata(any(), any(), any()) } returns Response.success(
                backendTrips.first()
            )
        }
        val tripsInteractorImpl = createTripInteractorImpl(
            backendTrips = backendTrips,
            apiClient = apiClient
        )
        runBlocking {
            tripsInteractorImpl.refreshTrips()
            tripsInteractorImpl.addPhotoToOrder("1", "")
            tripsInteractorImpl.updateOrderNote("1", "Note")
            tripsInteractorImpl.completeOrder("1")
            tripsInteractorImpl.addPhotoToOrder("2", "")
            tripsInteractorImpl.updateOrderNote("2", "Note")
            tripsInteractorImpl.cancelOrder("2")
            tripsInteractorImpl.addPhotoToOrder("3", "")
            tripsInteractorImpl.completeOrder("3")
            tripsInteractorImpl.updateOrderNote("4", "Note")
            tripsInteractorImpl.completeOrder("4")
            tripsInteractorImpl.completeOrder("5")

            val list = mutableListOf<Metadata>()
            coVerifyAll {
                apiClient.getTrips()
                apiClient.completeOrder(any(), any())
                apiClient.cancelOrder(any(), any())
                apiClient.updateOrderMetadata("1", "3", capture(list))
                apiClient.updateOrderMetadata("2", "3", capture(list))
                apiClient.updateOrderMetadata("3", "3", capture(list))
                apiClient.updateOrderMetadata("4", "3", capture(list))
            }
            list[0].let {
                assertEquals("Note", it.visitsAppMetadata.note)
                assertEquals(1, it.visitsAppMetadata.photos!!.size)
            }
            list[1].let {
                assertEquals("Note", it.visitsAppMetadata.note)
                assertEquals(1, it.visitsAppMetadata.photos!!.size)
            }
            list[2].let {
                assertEquals(1, it.visitsAppMetadata.photos!!.size)
            }
            list[3].let {
                assertEquals("Note", it.visitsAppMetadata.note)
            }
        }
    }

    @Test
    fun `it should create an order with null address if address is blank`() {
        var slot: String? = "default"
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { createTrip(any(), any()) } coAnswers {
                slot = secondArg()
                createBaseTrip()
            }
        }
        val tripsInteractor = createTripInteractorImpl(
            apiClient = apiClient,
        )
        runBlocking {
            tripsInteractor.createTrip(LatLng(1.1, 1.1), " ")
        }
        coVerifyAll {
            apiClient.createTrip(any(), any())
        }
        assertEquals(null, slot)
    }

    companion object {
        fun createTripsStorage(): TripsStorage {
            return object : TripsStorage {
                var trips: List<Trip> = listOf()

                override suspend fun saveTrips(trips: List<Trip>) {
                    this.trips = trips
                }

                override suspend fun getTrips(): List<Trip> {
                    return trips
                }
            }
        }

        fun createMockApiClient(
            backendTrips: List<RemoteTrip> = listOf(),
            additionalConfig: (ApiClient) -> Unit = {}
        ): ApiClient {
            return mockk {
                coEvery { getTrips() } returns backendTrips
                coEvery { completeOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { cancelOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { snoozeOrder(any(), any()) } returns JustSuccess
                coEvery { updateOrderMetadata(any(), any(), any()) } answers {
                    var trip = backendTrips.first { it.orders!!.any { it.id == firstArg() } }.copy()
                    trip = trip.copy(orders = trip.orders!!.map {
                        if (it.id == firstArg()) {
                            it.copy(
                                _metadata = thirdArg<Metadata>().copy(visitsAppMetadata = thirdArg<Metadata>().visitsAppMetadata.copy())
                                    .toMap()
                            )
                        } else {
                            it
                        }
                    })
                    Response.success(trip)
                }
                additionalConfig.invoke(this)
            }
        }

        fun createTripInteractorImpl(
            tripStorage: TripsStorage = mockk {
                coEvery { getTrips() } returns listOf()
                coEvery { saveTrips(any()) } returns Unit
            },
            backendTrips: List<RemoteTrip> = listOf(),
            apiClient: ApiClient = createMockApiClient(backendTrips),
            hyperTrackService: HyperTrackService = mockk(),
            queueInteractor: PhotoUploadQueueInteractor = mockk(relaxed = true) {},
            tripsRepository: TripsRepository = TripsRepositoryImpl(
                apiClient,
                tripStorage,
                TestCoroutineScope(),
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
            allowRefresh: () -> Boolean = { true }
        ): TripsInteractorImpl {
            return object : TripsInteractorImpl(
                mockk {
                    every { value } returns mockk {
                        every { isSdkTracking() } returns true
                    }
                },
                tripsRepository,
                apiClient,
                queueInteractor,
                mockk(relaxed = true) {},
                mockk(relaxed = true) {},
                crashReportsProvider = crashReportsProvider(),
                Dispatchers.Main,
                TestCoroutineScope()
            ) {
                override suspend fun refreshTrips() {
                    if (allowRefresh.invoke()) {
                        super.refreshTrips()
                    }
                }
            }
        }

        fun createBasePhotoForUpload(
            photoId: String = "1 " + Math.random(),
            filePath: String = "",
            state: PhotoUploadingState = PhotoUploadingState.NOT_UPLOADED
        ): PhotoForUpload {
            return PhotoForUpload(
                photoId,
                filePath,
                "",
                state,
            )
        }
    }


}
