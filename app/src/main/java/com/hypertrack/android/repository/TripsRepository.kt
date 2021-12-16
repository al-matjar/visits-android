package com.hypertrack.android.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.OrderCreationParams
import com.hypertrack.android.api.Trip
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.interactors.PhotoUploadingState
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.models.Order
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.toMap
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.logistics.android.github.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

interface TripsRepository {
    val trips: LiveData<List<LocalTrip>>
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    suspend fun refreshTrips()
    suspend fun createTrip(latLng: LatLng, address: String?): TripCreationResult
    suspend fun updateLocalOrder(orderId: String, updateFun: (LocalOrder) -> Unit)
    suspend fun completeTrip(tripId: String): SimpleResult
    suspend fun addOrderToTrip(tripId: String, orderParams: OrderCreationParams): Trip
}

class TripsRepositoryImpl(
    private val apiClient: ApiClient,
    private val tripsStorage: TripsStorage,
    private val hyperTrackService: HyperTrackService,
    private val coroutineScope: CoroutineScope,
    private val isPickUpAllowed: Boolean,
) : TripsRepository {

    override val trips = MutableLiveData<List<LocalTrip>>()

    private var tripsInitialized = false

    init {
        trips.observeForever {
            if (tripsInitialized) {
                coroutineScope.launch {
                    tripsStorage.saveTrips(it)
                }
            }
            tripsInitialized = true
        }
        coroutineScope.launch {
            trips.postValue(tripsStorage.getTrips())
        }
    }

    private val orderFactory = OrderFactory()
    private val legacyOrderFactory = LegacyOrderFactory()

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun refreshTrips() {
        try {
            val remoteTrips = apiClient.getTrips()
            val newTrips = mapTripsFromRemote(remoteTrips)
            trips.postValue(newTrips)
        } catch (e: Exception) {
            errorFlow.emit(Consumable((e)))
        }
    }

    override suspend fun createTrip(latLng: LatLng, address: String?): TripCreationResult {
        try {
            val trip = apiClient.createTrip(latLng, address)
            onTripCreated(trip)
            return TripCreationSuccess()
        } catch (e: Exception) {
            return TripCreationError(e)
        }
    }

    override suspend fun completeTrip(tripId: String): SimpleResult {
        return apiClient.completeTrip(tripId)
    }

    override suspend fun updateLocalOrder(orderId: String, updateFun: (LocalOrder) -> Unit) {
        trips.postValue(trips.value!!.map { localTrip ->
            localTrip.apply {
                orders = orders.map {
                    if (it.id == orderId) {
                        updateFun.invoke(it)
                        it
                    } else {
                        it
                    }
                }.toMutableList()
            }
        })
    }

    override suspend fun addOrderToTrip(tripId: String, orderParams: OrderCreationParams): Trip {
        val trip = apiClient.addOrderToTrip(tripId, orderParams)
        updateTrip(trip)
        return trip
    }

    private suspend fun updateTrip(remoteTrip: Trip) {
        trips.postValue(trips.value!!.map {
            if (it.id == remoteTrip.id) {
                localTripFromRemote(
                    remoteTrip,
                    localOrdersFromRemote(
                        remoteTrip.orders ?: listOf(),
                        it.orders,
                        orderFactory
                    )
                )
            } else {
                it
            }
        })
    }

    private suspend fun onTripCreated(trip: Trip) {
        trips.postValue(
            (trips.value ?: listOf()).toMutableList()
                .apply {
                    add(
                        localTripFromRemote(
                            trip, localOrdersFromRemote(
                                trip.orders ?: listOf(),
                                listOf(),
                                orderFactory
                            )
                        )
                    )
                })
    }

    private suspend fun mapTripsFromRemote(remoteTrips: List<Trip>): List<LocalTrip> {
        val legacyTrip = remoteTrips.firstOrNull {
            it.orders.isNullOrEmpty() && it.status == TripStatus.ACTIVE.value
        }
        if (legacyTrip != null) {
            //legacy mode for v1 trips
            //destination = order, order.id = trip.id
            //todo handle case if not loaded from database yet
            val localTrip = localTripFromLegacyRemoteTrip(legacyTrip)
            return listOf(localTrip)
        } else {
            val localTrips = tripsStorage.getTrips().toMap { it.id }
            val newTrips = remoteTrips.map { remoteTrip ->
                if (remoteTrip.id in localTrips.keys) {
                    val localTrip = localTrips.getValue(remoteTrip.id!!)
                    val remoteOrders = (remoteTrip.orders ?: listOf())
                    val localOrders = localTrip.orders

                    val orders = localOrdersFromRemote(remoteOrders, localOrders, orderFactory)

                    return@map localTripFromRemote(remoteTrip, orders)
                } else {
                    localTripFromRemote(
                        remoteTrip,
                        localOrdersFromRemote(
                            remoteTrip.orders ?: listOf(),
                            listOf(),
                            orderFactory
                        )
                    )
                }
            }
            return newTrips
        }
    }

    private suspend fun localTripFromLegacyRemoteTrip(legacyTrip: Trip): LocalTrip {
        val oldLocalOrders = trips.value!!.firstOrNull { it.id == legacyTrip.id }
            ?.orders ?: listOf()
        return localTripFromRemote(
            legacyTrip,
            localOrdersFromRemote(
                listOf(createLegacyRemoteOrder(legacyTrip)),
                oldLocalOrders,
                legacyOrderFactory
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun localTripFromRemote(remoteTrip: Trip, newLocalOrders: List<LocalOrder>): LocalTrip {
        return LocalTrip(
            remoteTrip.id!!,
            TripStatus.fromString(remoteTrip.status),
            ((remoteTrip.metadata ?: mapOf<String, String>())
                .filter { it.value is String } as Map<String, String>)
                .toMutableMap()
                .apply {
                    if (newLocalOrders.any { it.legacy } && MyApplication.DEBUG_MODE) {
                        put("legacy (debug)", "true")
                    }
                },
            newLocalOrders.toMutableList(),
            remoteTrip.views
        )
    }

    private suspend fun localOrdersFromRemote(
        remoteOrders: List<Order>,
        oldLocalOrders: List<LocalOrder>,
        orderFactory: LocalOrderFactory
    ): List<LocalOrder> {
        val localOrdersMap = oldLocalOrders.toMap { it.id }
        return remoteOrders.map {
            val localOrder = localOrdersMap.get(it.id)
            val res = orderFactory.create(it, localOrder)
            res
        }
    }

    private fun createLegacyRemoteOrder(trip: Trip): Order {
        return Order(
            id = trip.id!!,
            destination = trip.destination!!,
            _status = OrderStatus.ONGOING.value,
            scheduledAt = null,
            completedAt = null,
            estimate = trip.estimate,
            _metadata = mapOf(),
        )
    }

    inner class OrderFactory : LocalOrderFactory {
        @Suppress("RedundantIf")
        override suspend fun create(order: Order, localOrder: LocalOrder?): LocalOrder {
            val remoteMetadata = Metadata.deserialize(order.metadata)
            val localPhotosMap = localOrder?.photos?.toMap { it.photoId } ?: mapOf()
            val resPhotos = mutableSetOf<PhotoForUpload>().apply {
                addAll(localOrder?.photos ?: listOf())
                (remoteMetadata.visitsAppMetadata.photos ?: listOf()).forEach {
                    if (!localPhotosMap.containsKey(it)) {
                        val loadedImage = apiClient.getImageBase64(it)

                        //todo cache
                        PhotoForUpload(
                            it,
                            null,
                            loadedImage,
                            state = PhotoUploadingState.UPLOADED
                        )
                    }
                }
            }

            return LocalOrder.fromRemote(
                order,
                isPickedUp = localOrder?.isPickedUp ?: if (isPickUpAllowed) {
                    false
                } else {
                    true //if pick up not allowed, order created as already picked up
                },
                note = localOrder?.note,
                photos = resPhotos,
                metadata = remoteMetadata
            )
        }
    }

    inner class LegacyOrderFactory : LocalOrderFactory {
        @Suppress("RedundantIf")
        override suspend fun create(order: Order, localOrder: LocalOrder?): LocalOrder {
            val res = LocalOrder.fromRemote(
                order,
                isPickedUp = localOrder?.isPickedUp ?: if (isPickUpAllowed) {
                    false
                } else {
                    true //if pick up not allowed, order created as already picked up
                },
                note = localOrder?.note,
                legacy = true,
                photos = localOrder?.photos ?: mutableSetOf(),
                metadata = Metadata.deserialize(order.metadata),
                status = if (localOrder?.status == OrderStatus.COMPLETED) {
                    OrderStatus.COMPLETED
                } else null,
            )
            return res
        }
    }

}


interface LocalOrderFactory {
    suspend fun create(order: Order, localOrder: LocalOrder?): LocalOrder
}

sealed class TripCreationResult
class TripCreationSuccess : TripCreationResult()
class TripCreationError(val exception: Exception) : TripCreationResult()
