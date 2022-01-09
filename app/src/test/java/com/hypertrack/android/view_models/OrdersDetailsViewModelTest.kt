package com.hypertrack.android.view_models

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.MainCoroutineScopeRule
import com.hypertrack.android.api.OrderCompletionSuccess
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.interactors.*
import com.hypertrack.android.interactors.TripInteractorTest.Companion.createMockApiClient
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.models.Order
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.common.adapters.formatUnderscore
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel.Companion.REQUEST_IMAGE_CAPTURE
import com.hypertrack.logistics.android.github.R
import io.mockk.*
import junit.framework.Assert.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.File

@Suppress("EXPERIMENTAL_API_USAGE")
class OrdersDetailsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `it should show correct view state for order`() {
        runBlocking {
            val tripsInteractor: TripsInteractor = createTripsInteractorMock(orderSet = {
                every { it.getOrderLiveData("ONGOING") } returns MutableLiveData<LocalOrder>(
                    LocalOrder(
                        createBaseOrder(status = OrderStatus.ONGOING),
                        note = null,
                        metadata = null,
                        isPickedUp = false
                    )
                )
                every { it.getOrderLiveData("COMPLETED") } returns MutableLiveData<LocalOrder>(
                    LocalOrder(
                        createBaseOrder(status = OrderStatus.COMPLETED),
                        note = null,
                        metadata = null,
                        isPickedUp = false
                    )
                )
                every { it.getOrderLiveData("CANCELED") } returns MutableLiveData<LocalOrder>(
                    LocalOrder(
                        createBaseOrder(status = OrderStatus.CANCELED),
                        note = null,
                        metadata = null,
                        isPickedUp = false
                    )
                )
                every { it.getOrderLiveData("SNOOZED") } returns MutableLiveData<LocalOrder>(
                    LocalOrder(
                        createBaseOrder(status = OrderStatus.SNOOZED),
                        note = null,
                        metadata = null,
                        isPickedUp = false
                    )
                )
            })

            fun assertMetadata(expected: String?, key: String, md: List<KeyValueItem>) {
                assertEquals(expected, getFromMetadata(key, md)?.toLowerCase())
            }

            var isPickUpAllowed = true
            createVm("ONGOING", tripsInteractor, isPickUpAllowed).let {
                assertEquals(true, it.isNoteEditable.observeAndGetValue())
                assertEquals(true, it.showCompleteButtons.observeAndGetValue())
                assertEquals(false, it.showPickUpButton.observeAndGetValue())
                assertEquals(true, it.showAddPhoto.observeAndGetValue())
                assertEquals(true, it.showSnoozeButton.observeAndGetValue())
                assertEquals(false, it.showUnsnoozeButton.observeAndGetValue())
                val md = it.metadata.observeAndGetValue()
                assertMetadata(OrderStatus.ONGOING.value, "order_status", md)
                assertMetadata("false", "order_picked_up", md)
            }

            isPickUpAllowed = false
            createVm("ONGOING", tripsInteractor, isPickUpAllowed).let {
                assertEquals(true, it.isNoteEditable.observeAndGetValue())
                assertEquals(true, it.showCompleteButtons.observeAndGetValue())
                assertEquals(false, it.showPickUpButton.observeAndGetValue())
                assertEquals(true, it.showAddPhoto.observeAndGetValue())
                assertEquals(true, it.showSnoozeButton.observeAndGetValue())
                assertEquals(false, it.showUnsnoozeButton.observeAndGetValue())
                val md = it.metadata.observeAndGetValue()
                assertMetadata(OrderStatus.ONGOING.value, "order_status", md)
            }

            createVm("COMPLETED", tripsInteractor, isPickUpAllowed).let {
                assertEquals(false, it.isNoteEditable.observeAndGetValue())
                assertEquals(false, it.showCompleteButtons.observeAndGetValue())
                assertEquals(false, it.showPickUpButton.observeAndGetValue())
                assertEquals(false, it.showAddPhoto.observeAndGetValue())
                assertEquals(false, it.showSnoozeButton.observeAndGetValue())
                assertEquals(false, it.showUnsnoozeButton.observeAndGetValue())
                val md = it.metadata.observeAndGetValue()
                assertMetadata(OrderStatus.COMPLETED.value, "order_status", md)
            }

            createVm("CANCELED", tripsInteractor, isPickUpAllowed).let {
                assertEquals(false, it.isNoteEditable.observeAndGetValue())
                assertEquals(false, it.showCompleteButtons.observeAndGetValue())
                assertEquals(false, it.showPickUpButton.observeAndGetValue())
                assertEquals(false, it.showAddPhoto.observeAndGetValue())
                assertEquals(false, it.showSnoozeButton.observeAndGetValue())
                assertEquals(false, it.showUnsnoozeButton.observeAndGetValue())
                val md = it.metadata.observeAndGetValue()
                assertMetadata(OrderStatus.CANCELED.value, "order_status", md)
            }

            createVm("SNOOZED", tripsInteractor, isPickUpAllowed).let {
                assertEquals(false, it.isNoteEditable.observeAndGetValue())
                assertEquals(false, it.showCompleteButtons.observeAndGetValue())
                assertEquals(false, it.showPickUpButton.observeAndGetValue())
                assertEquals(false, it.showAddPhoto.observeAndGetValue())
                assertEquals(false, it.showSnoozeButton.observeAndGetValue())
                assertEquals(true, it.showUnsnoozeButton.observeAndGetValue())
                val md = it.metadata.observeAndGetValue()
                assertMetadata(OrderStatus.SNOOZED.value, "order_status", md)
            }
        }
    }

    @Test
    fun `it should update order state on complete button click`() {
        val backendOrders = listOf<Order>(
            createBaseOrder().copy(
                id = "ONGOING",
                _status = OrderStatus.ONGOING.value
            ),
        )

        var allowRefresh = true
        val pickUpAllowed = true
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(createBaseTrip().copy(orders = backendOrders)),
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        ) {
            allowRefresh
        }
        runBlocking {
            tripsInteractor.refreshTrips()
            allowRefresh = false

            createVm("ONGOING", tripsInteractor, pickUpAllowed).let {
                it.onCompleteClicked()

                assertEquals(
                    OrderStatus.COMPLETED.value,
                    getFromMetadata("order_status", it.metadata.observeAndGetValue())
                )
                assertFalse(it.showCompleteButtons.observeAndGetValue())
                assertFalse(it.showPickUpButton.observeAndGetValue())
                assertFalse(it.isNoteEditable.observeAndGetValue())
                assertFalse(it.showAddPhoto.observeAndGetValue())
                assertNull(getFromMetadata("order_picked_up", it.metadata.observeAndGetValue()))
            }
        }
    }

    @Test
    fun `it should update order state on cancel button click`() {
        val backendOrders = listOf<Order>(
            createBaseOrder().copy(
                id = "ONGOING",
                _status = OrderStatus.ONGOING.value
            ),
        )

        var allowRefresh = true
        val pickUpAllowed = true
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(createBaseTrip().copy(orders = backendOrders)),
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        ) {
            allowRefresh
        }
        runBlocking {
            tripsInteractor.refreshTrips()
            allowRefresh = false

            createVm("ONGOING", tripsInteractor, pickUpAllowed).let {
                it.onCancelClicked()

                assertEquals(
                    OrderStatus.CANCELED.value,
                    getFromMetadata("order_status", it.metadata.observeAndGetValue())
                )
                assertFalse(it.showCompleteButtons.observeAndGetValue())
                assertFalse(it.showPickUpButton.observeAndGetValue())
                assertFalse(it.isNoteEditable.observeAndGetValue())
                assertFalse(it.showAddPhoto.observeAndGetValue())
                assertNull(getFromMetadata("order_picked_up", it.metadata.observeAndGetValue()))
            }
        }
    }

    @Test
    fun `it should update order state on snooze button click`() {
        val backendOrders = listOf<Order>(
            createBaseOrder().copy(
                id = "ONGOING",
                _status = OrderStatus.ONGOING.value
            ),
        )

        var allowRefresh = true
        val pickUpAllowed = true
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(createBaseTrip().copy(orders = backendOrders)),
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        ) {
            allowRefresh
        }
        runBlocking {
            tripsInteractor.refreshTrips()
            allowRefresh = false

            createVm("ONGOING", tripsInteractor, pickUpAllowed).let {
                it.onCancelClicked()

                assertEquals(
                    OrderStatus.CANCELED.value,
                    getFromMetadata("order_status", it.metadata.observeAndGetValue())
                )
                assertFalse(it.showCompleteButtons.observeAndGetValue())
                assertFalse(it.showPickUpButton.observeAndGetValue())
                assertFalse(it.isNoteEditable.observeAndGetValue())
                assertFalse(it.showAddPhoto.observeAndGetValue())
                assertNull(getFromMetadata("order_picked_up", it.metadata.observeAndGetValue()))
            }
        }
    }

    @Test
    fun `it should save note on exit`() {
        val pickUpAllowed = true
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            backendTrips = listOf(createBaseTrip().copy(orders = listOf(createBaseOrder().copy(id = "1")))),
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        )

        runBlocking {
            val vm = createVm("1", tripsInteractor, pickUpAllowed)
            tripsInteractor.refreshTrips()

            vm.let {
                it.onExit("Note")
            }

            runBlocking {
                assertEquals("Note", vm.note.observeAndGetValue())
            }
        }
    }

    @Test
    fun `it should persist order photos`() {
        runBlocking {
            var trip = createBaseTrip().copy(
                id = "t1",
                status = TripStatus.ACTIVE.value,
                orders = listOf(
                    createBaseOrder().copy(id = "1")
                )
            )
            val queueInteractor = object : PhotoUploadQueueInteractor {
                override fun addToQueue(photo: PhotoForUpload) {
                    queue.postValue(queue.value!!.toMutableMap().apply {
                        put(photo.photoId, photo.apply {
                            state = PhotoUploadingState.UPLOADED
                        })
                    })
                }

                override fun retry(photoId: String) {
                }

                override val errorFlow = MutableSharedFlow<Consumable<Exception>>()
                override val queue = MutableLiveData<Map<String, PhotoForUpload>>(mapOf())
            }
            assertTrue(queueInteractor.queue.value!!.isEmpty())
            val tripsInteractor = TripInteractorTest.createTripInteractorImpl(
                apiClient = mockk {
                    coEvery { getTrips() } answers {
                        listOf(trip)
                    }
                    coEvery {
                        completeOrder(
                            any(),
                            any()
                        )
                    } returns com.hypertrack.android.api.OrderCompletionSuccess
                    coEvery {
                        cancelOrder(
                            any(),
                            any()
                        )
                    } returns com.hypertrack.android.api.OrderCompletionSuccess
                    coEvery { updateOrderMetadata(any(), any(), any()) } answers {
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
                },
                queueInteractor = queueInteractor,
                tripStorage = TripInteractorTest.createTripsStorage()
            )
            tripsInteractor.refreshTrips()


            createVm("1", tripsInteractor, false, queueInteractor).let {
                it.onAddPhotoClicked(mockk(relaxed = true), "")
                it.onActivityResult(
                    REQUEST_IMAGE_CAPTURE,
                    AppCompatActivity.RESULT_OK,
                    null
                )

                runBlocking {
                    tripsInteractor.getOrder("1")!!.photos.let {
                        assertEquals(1, it.size)
                    }

                    tripsInteractor.refreshTrips()

                    tripsInteractor.getOrder("1")!!.photos.let {
                        assertEquals(1, it.size)
                    }
                }

            }
        }
    }

    @Test
    fun `it should show local note`() {
        val pickUpAllowed = true
        val order = createBaseOrder().copy(
            id = "1",
            _metadata = mapOf(Metadata.VISITS_APP_KEY to mapOf("note" to "Note"))
        )
        val apiClient = createMockApiClient(
            backendTrips = listOf(
                createBaseTrip().copy(
                    id = "1",
                    orders = listOf(order)
                )
            )
        )
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            apiClient = apiClient,
            tripStorage = mockk() {
                coEvery { getTrips() } returns listOf(
                    LocalTrip(
                        "1", TripStatus.ACTIVE, mapOf(), mutableListOf(
                            LocalOrder(
                                order,
                                metadata = null,
                                note = "Note_local"
                            )
                        )
                    )
                )
                coEvery { saveTrips(any()) } returns Unit
            },
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        )

        runBlocking {
            val vm = createVm("1", tripsInteractor, pickUpAllowed)
            tripsInteractor.refreshTrips()

            vm.let {
                assertEquals("Note_local", vm.note.observeAndGetValue())
            }

        }
    }

    @Test
    fun `it should set local note to remote if local is null`() {
        val pickUpAllowed = true
        val order = createBaseOrder().copy(
            id = "1",
            _metadata = mapOf(Metadata.VISITS_APP_KEY to mapOf("note" to "Note"))
        )
        val apiClient = createMockApiClient(
            backendTrips = listOf(
                createBaseTrip().copy(
                    id = "1",
                    orders = listOf(order)
                )
            )
        )
        val tripsInteractor: TripsInteractor = TripInteractorTest.createTripInteractorImpl(
            apiClient = apiClient,
            tripStorage = mockk() {
                coEvery { getTrips() } returns listOf(
                    LocalTrip(
                        "1", TripStatus.ACTIVE, mapOf(), mutableListOf(
                            LocalOrder(
                                order,
                                metadata = null,
                                note = null
                            )
                        )
                    )
                )
                coEvery { saveTrips(any()) } returns Unit
            },
            accountRepository = mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed }
        )

        runBlocking {
            val vm = createVm("1", tripsInteractor, pickUpAllowed)
            tripsInteractor.refreshTrips()

            vm.let {
                assertEquals("Note", vm.note.observeAndGetValue())
            }

        }
    }

    @Test
    fun `it should upload order photos`() {
        runBlocking {
            var trip = createBaseTrip().copy(
                id = "t1",
                status = TripStatus.ACTIVE.value,
                orders = listOf(
                    createBaseOrder().copy(id = "1")
                )
            )
            val apiClient = mockk<ApiClient> {
                coEvery { getTrips() } answers {
                    listOf(trip)
                }
                coEvery { completeOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { cancelOrder(any(), any()) } returns OrderCompletionSuccess
                coEvery { updateOrderMetadata(any(), any(), any()) } answers {
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
            }
            val queueInteractor = object : PhotoUploadQueueInteractor {
                override fun addToQueue(photo: PhotoForUpload) {
                    queue.postValue(queue.value!!.toMutableMap().apply {
                        put(photo.photoId, photo.apply {
                            state = PhotoUploadingState.UPLOADED
                        })
                    })
                }

                override fun retry(photoId: String) {
                }

                override val errorFlow = MutableSharedFlow<Consumable<Exception>>()
                override val queue = MutableLiveData<Map<String, PhotoForUpload>>(mapOf())
            }
            assertTrue(queueInteractor.queue.value!!.isEmpty())
            val tripsInteractor = TripInteractorTest.createTripInteractorImpl(
                apiClient = apiClient,
                queueInteractor = queueInteractor,
                tripStorage = TripInteractorTest.createTripsStorage()
            )
            tripsInteractor.refreshTrips()


            createVm("1", tripsInteractor, false, queueInteractor).let {
                val activity = mockk<Activity>(relaxed = true)

                it.onAddPhotoClicked(activity, "Note")

                verify { activity.startActivityForResult(any(), any()) }
                assertEquals(
                    "Note",
                    tripsInteractor.currentTrip.observeAndGetValue()!!.orders.first().note
                )

                it.onActivityResult(
                    REQUEST_IMAGE_CAPTURE,
                    AppCompatActivity.RESULT_OK,
                    null
                )

                it.onActivityResult(
                    REQUEST_IMAGE_CAPTURE,
                    AppCompatActivity.RESULT_OK,
                    null
                )

                tripsInteractor.getOrder("1")!!.photos.let {
                    assertEquals(2, it.size)
                }

                it.photos.observeAndGetValue().let {
                    assertEquals(PhotoUploadingState.UPLOADED, it[0].state)
                }
            }
        }
    }

    @Test
    fun `it should retry upload on order photo click (if error)`() {
        runBlocking {
            val ld = MutableLiveData<Map<String, PhotoForUpload>>(
                mapOf(
                    "1" to TripInteractorTest.createBasePhotoForUpload(
                        "1",
                        "",
                        PhotoUploadingState.ERROR
                    ),
                    "2" to TripInteractorTest.createBasePhotoForUpload(
                        "2",
                        "",
                        PhotoUploadingState.NOT_UPLOADED
                    ),
                    "3" to TripInteractorTest.createBasePhotoForUpload(
                        "3",
                        "",
                        PhotoUploadingState.UPLOADED
                    ),
                )
            )
            val slot = slot<String>()
            val queueInteractor = mockk<PhotoUploadQueueInteractor>() {
                every { retry(capture(slot)) } returns Unit
                every { queue } returns ld
            }
            val tripsInteractor = createTripsInteractorMock(orderSet = {
                every { it.getOrderLiveData("1") } returns MutableLiveData(
                    LocalOrder(
                        createBaseOrder(),
                        true,
                        null,
                        Metadata.empty().apply {
                            visitsAppMetadata.apply {
                                photos = listOf("1", "2", "3")
                            }
                        },
                        false,
                        listOf("1", "2", "3").map {
                            TripInteractorTest.createBasePhotoForUpload(
                                it,
                                "",
                                state = PhotoUploadingState.NOT_UPLOADED
                            )
                        }.toMutableSet(),
                    )
                )
            })
            createVm("1", tripsInteractor, false, queueInteractor).let {
                it.photos.observeAndGetValue().let {
                    assertEquals(3, it.size)
                    assertEquals(PhotoUploadingState.ERROR, it.first { it.photoId == "1" }.state)
                    assertEquals(
                        PhotoUploadingState.NOT_UPLOADED,
                        it.first { it.photoId == "2" }.state
                    )
                    assertEquals(PhotoUploadingState.UPLOADED, it.first { it.photoId == "3" }.state)
                }

                it.onPhotoClicked("1")
                it.onPhotoClicked("2")
                it.onPhotoClicked("3")

                coVerifyAll {
                    queueInteractor.queue
                    queueInteractor.queue
                    queueInteractor.retry("1")
                }
            }
        }
    }

    companion object {
        fun createTripsInteractorMock(
            orderSet: ((TripsInteractor) -> Unit) = {
                every { it.getOrderLiveData(any()) } returns MutableLiveData<LocalOrder>(
                    LocalOrder(
                        createBaseOrder(),
                        note = null,
                        metadata = null,
                        isPickedUp = false
                    )
                )
            }
        ): TripsInteractor {
            return mockk(relaxed = true) {
                orderSet.invoke(this)
                every { errorFlow } returns MutableSharedFlow()
                every { currentTrip } returns MutableLiveData<LocalTrip>()
            }
        }

        fun getFromMetadata(key: String, metadata: List<KeyValueItem>): String? {
            return metadata.firstOrNull {
                it.key == key.formatUnderscore()
            }?.value
        }

        fun createVm(
            id: String,
            tripsInteractor: TripsInteractor,
            pickUpAllowed: Boolean = false,
            photoUploadInteractor: PhotoUploadQueueInteractor = mockk(relaxed = true),
            ordersInteractor: OrdersInteractor = mockk(),
        ): OrderDetailsViewModel {
            return OrderDetailsViewModel(
                id,
                mockk(relaxed = true) {
                    every { osUtilsProvider } returns mockk(relaxed = true) {
                        every { stringFromResource(R.string.order_status) } returns "order_status"
                        every { stringFromResource(R.string.order_picked_up) } returns "order_picked_up"
                        every { cacheDir } returns File("nofile")
                        every { createImageFile() } returns File("nofile")
                    }
                },
                tripsInteractor,
                ordersInteractor,
                photoUploadInteractor,
                mockk() { coEvery { isPickUpAllowed } returns pickUpAllowed },
                mockk(relaxed = true),
            )
        }
    }

}