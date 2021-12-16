package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.common.util.toHotTransformation
import com.hypertrack.android.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException
import java.util.*

//todo separate
interface TripsInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val currentTrip: LiveData<LocalTrip?>
    val completedTrips: LiveData<List<LocalTrip>>
    fun getOrderLiveData(orderId: String): LiveData<LocalOrder>
    suspend fun refreshTrips()
    suspend fun cancelOrder(orderId: String): OrderCompletionResponse
    suspend fun completeOrder(orderId: String): OrderCompletionResponse
    fun getOrder(orderId: String): LocalOrder?
    suspend fun updateOrderNote(orderId: String, orderNote: String)
    fun updateOrderNoteAsync(orderId: String, orderNote: String)
    suspend fun setOrderPickedUp(orderId: String)
    suspend fun addPhotoToOrder(orderId: String, path: String)
    fun retryPhotoUpload(orderId: String, photoId: String)
    suspend fun createTrip(latLng: LatLng, address: String?): TripCreationResult
    suspend fun completeTrip(tripId: String): SimpleResult
    suspend fun addOrderToTrip(
        tripId: String,
        latLng: LatLng,
        address: String?
    ): AddOrderResult

    fun logState(): Map<String, Any?>
    suspend fun snoozeOrder(orderId: String): SimpleResult
    suspend fun unsnoozeOrder(orderId: String): SimpleResult
}

open class TripsInteractorImpl(
    private val tripsRepository: TripsRepository,
    private val apiClient: ApiClient,
    private val hyperTrackService: HyperTrackService,
    private val photoUploadInteractor: PhotoUploadQueueInteractor,
    private val imageDecoder: ImageDecoder,
    private val osUtilsProvider: OsUtilsProvider,
    private val ioDispatcher: CoroutineDispatcher,
    private val globalScope: CoroutineScope
) : TripsInteractor {

    override val completedTrips = Transformations.map(tripsRepository.trips) {
        it.filter { it.status != TripStatus.ACTIVE }
    }

    override val currentTrip = Transformations.map(tripsRepository.trips) {
        getCurrentTrip(it)
    }.toHotTransformation().liveData


    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun getOrderLiveData(orderId: String): LiveData<LocalOrder> {
        return Transformations.switchMap(tripsRepository.trips) {
            MutableLiveData<LocalOrder>().apply {
                getOrder(orderId).let { if (it != null) postValue(it) }
            }
        }
    }

    override suspend fun refreshTrips() {
        globalScope.launch {
            tripsRepository.refreshTrips()
        }
    }

    override fun getOrder(orderId: String): LocalOrder? {
        return tripsRepository.trips.value?.map { it.orders }?.flatten()
            ?.firstOrNull() { it.id == orderId }
    }

    override suspend fun completeOrder(orderId: String): OrderCompletionResponse {
        return if (hyperTrackService.isTracking.value == true) {
            withContext(globalScope.coroutineContext) {
                setOrderCompletionStatus(orderId, canceled = false)
            }
        } else {
            OrderCompletionFailure(NotClockedInException)
        }
    }

    override suspend fun snoozeOrder(orderId: String): SimpleResult {
        return if (hyperTrackService.isTracking.value == true) {
            withContext(globalScope.coroutineContext) {
                try {
                    currentTrip.value!!.let { trip ->
                        trip.getOrder(orderId = orderId)!!.let { order ->
                            if (!order.legacy) {
                                updateOrderMetadata(trip.id, order)
                                apiClient.snoozeOrder(orderId = orderId, tripId = trip.id)
                                    .let { res ->
                                        if (res is JustSuccess) {
                                            tripsRepository.updateLocalOrder(orderId) {
                                                it.status = OrderStatus.SNOOZED
                                            }
                                        }
                                        globalScope.launch {
                                            refreshTrips()
                                        }
                                        res
                                    }
                            } else {
                                JustFailure(IllegalArgumentException("Can't snooze legacy order"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    JustFailure(e)
                }
            }
        } else {
            JustFailure(NotClockedInException)
        }
    }

    override suspend fun unsnoozeOrder(orderId: String): SimpleResult {
        return try {
            onlyWhenClockedIn {
                currentTrip.value!!.let { trip ->
                    apiClient.unsnoozeOrder(tripId = trip.id, orderId = orderId).let { res ->
                        if (res is JustSuccess) {
                            tripsRepository.updateLocalOrder(orderId) {
                                it.status = OrderStatus.ONGOING
                            }
                        }
                        globalScope.launch {
                            refreshTrips()
                        }
                        res
                    }
                }
            }
        } catch (e: Exception) {
            JustFailure(e)
        }
    }

    override suspend fun cancelOrder(orderId: String): OrderCompletionResponse {
        if (hyperTrackService.isTracking.value == true) {
            return withContext(globalScope.coroutineContext) {
                setOrderCompletionStatus(orderId, canceled = true)
            }
        } else {
            return OrderCompletionFailure(NotClockedInException)
        }
    }

    override suspend fun updateOrderNote(orderId: String, orderNote: String) {
        try {
            tripsRepository.updateLocalOrder(orderId) {
                it.note = orderNote
            }
        } catch (e: Exception) {
            errorFlow.emit(Consumable(e))
        }
    }

    override fun updateOrderNoteAsync(orderId: String, orderNote: String) {
        globalScope.launch {
            updateOrderNote(orderId, orderNote)
        }
    }

    override suspend fun setOrderPickedUp(orderId: String) {
        globalScope.launch {
            //used only for legacy orders, so orderId is tripId
            hyperTrackService.sendPickedUp(orderId, "trip_id")
            tripsRepository.updateLocalOrder(orderId) {
                it.isPickedUp = true
            }
        }
    }

    override suspend fun addPhotoToOrder(orderId: String, path: String) {
        globalScope.launch {
            try {
                val generatedImageId = UUID.randomUUID().toString()

                val previewMaxSideLength: Int = (200 * osUtilsProvider.screenDensity).toInt()
                withContext(ioDispatcher) {
                    val bitmap = imageDecoder.readBitmap(path, previewMaxSideLength)
                    val photo = PhotoForUpload(
                        photoId = generatedImageId,
                        filePath = path,
                        base64thumbnail = osUtilsProvider.bitmapToBase64(bitmap),
                        state = PhotoUploadingState.NOT_UPLOADED
                    )
                    tripsRepository.updateLocalOrder(orderId) {
//                it.photos.add(photo.photoId)
                        it.photos.add(photo)
                    }
                    photoUploadInteractor.addToQueue(photo)
                }
            } catch (e: Exception) {
                errorFlow.emit(Consumable(e))
            }
        }
    }

    override fun retryPhotoUpload(orderId: String, photoId: String) {
        photoUploadInteractor.retry(photoId)
    }

    override suspend fun createTrip(latLng: LatLng, address: String?): TripCreationResult {
        return withContext(globalScope.coroutineContext) {
            tripsRepository.createTrip(latLng, address.nullIfBlank())
        }
    }

    override suspend fun completeTrip(tripId: String): SimpleResult {
        return try {
            tripsRepository.completeTrip(tripId).let {
                when (it) {
                    JustSuccess -> {
                        refreshTrips()
                        it
                    }
                    is JustFailure -> it
                }
            }

        } catch (e: Exception) {
            JustFailure(e)
        }
    }

    override suspend fun addOrderToTrip(
        tripId: String,
        latLng: LatLng,
        address: String?
    ): AddOrderResult {
        if (tripsRepository.trips.value!!.first { it.id == tripId }.isLegacy()) {
            throw IllegalArgumentException("Can't add an order to a legacy v1 trip")
        }
        return withContext(globalScope.coroutineContext) {
            try {
                tripsRepository.addOrderToTrip(
                    tripId, OrderCreationParams(
                        UUID.randomUUID().toString(),
                        TripDestination(latLng, address)
                    )
                )
                AddOrderSuccess
            } catch (e: Exception) {
                AddOrderError(e)
            }
        }
    }

    private suspend fun setOrderCompletionStatus(
        orderId: String,
        canceled: Boolean
    ): OrderCompletionResponse {
        try {
            currentTrip.value!!.let { trip ->
                trip.getOrder(orderId)!!.let { order ->
                    if (order.legacy) {
                        //legacy v1 trip, destination is order, order.id is trip.id
                        hyperTrackService.sendCompletionEvent(order, canceled)
                        tripsRepository.updateLocalOrder(orderId) {
                            it.status = if (!canceled) {
                                OrderStatus.COMPLETED
                            } else {
                                OrderStatus.CANCELED
                            }
                        }
                        globalScope.launch {
                            refreshTrips()
                        }
                        return OrderCompletionSuccess
                        //todo completion is disabled regarding to Indiabulls use-case
//                        val res = apiClient.completeTrip(order.id)
//                        when (res) {
//                            JustSuccess -> {
//                                updateCurrentTripOrderStatus(
//                                    orderId, if (!canceled) {
//                                        OrderStatus.COMPLETED
//                                    } else {
//                                        OrderStatus.CANCELED
//                                    }
//                                )
//                                return OrderCompletionSuccess
//                            }
//                            is TripCompletionError -> {
//                                return OrderCompletionFailure(res.error as Exception)
//                            }
//                        }
                    } else {
                        updateOrderMetadata(trip.id, order)

                        val res = if (!canceled) {
                            apiClient.completeOrder(orderId = orderId, tripId = trip.id)
                        } else {
                            apiClient.cancelOrder(orderId = orderId, tripId = trip.id)
                        }
                        if (res is OrderCompletionSuccess) {
                            tripsRepository.updateLocalOrder(orderId) {
                                it.status = if (!canceled) {
                                    OrderStatus.COMPLETED
                                } else {
                                    OrderStatus.CANCELED
                                }
                            }
                        }
                        globalScope.launch {
                            refreshTrips()
                        }
                        return res
                    }
                }
            }
        } catch (e: Exception) {
            return OrderCompletionFailure(e)
        }
    }

    private fun getCurrentTrip(trips: List<LocalTrip>): LocalTrip? {
        return trips.firstOrNull {
            it.status == TripStatus.ACTIVE
        }
    }

    private suspend fun updateOrderMetadata(tripId: String, order: LocalOrder) {
        val orderId = order.id
        val metadata = (order._metadata ?: Metadata.empty())
        if (
            metadata.visitsAppMetadata.note != order.note
            || !(metadata.visitsAppMetadata.photos ?: listOf())
                .hasSameContent(order.photos.map { it.photoId }.toList())
        ) {
            val mdRes = apiClient.updateOrderMetadata(
                orderId = orderId,
                tripId = tripId,
                metadata = metadata.apply {
                    visitsAppMetadata.note = order.note
                    visitsAppMetadata.photos = order.photos.map { it.photoId }
                }
            )
            if (!mdRes.isSuccessful) {
                throw HttpException(mdRes)
            }
        }
    }

    override fun logState(): Map<String, Any?> {
        return mapOf(
            "currentTrip" to currentTrip.value?.let { trip ->
                mapOf(
                    "id" to trip.id,
                    "status" to trip.status,
                    "orders" to trip.orders.map { order ->
                        mapOf(
                            "id" to order.id,
                            "status" to order.status,
                        )
                    }
                )
            }
        )

    }

    private suspend fun <T> onlyWhenClockedIn(code: (suspend () -> T)): T {
        return if (hyperTrackService.isTracking.value == true) {
            code.invoke()
        } else {
            throw NotClockedInException
        }
    }

}

fun <T> List<T>.hasSameContent(list: List<T>): Boolean {
    if (this.isEmpty() && list.isEmpty()) return true
    return containsAll(list) && list.containsAll(this)
}

sealed class AddOrderResult
object AddOrderSuccess : AddOrderResult()
class AddOrderError(val e: Exception) : AddOrderResult()

object NotClockedInException : Exception()
