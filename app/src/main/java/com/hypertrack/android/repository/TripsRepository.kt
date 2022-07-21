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
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.util.toMap
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.exception.SimpleException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

interface TripsRepository {
    val trips: LiveData<List<LocalTrip>>
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    suspend fun refreshTrips()
    suspend fun createTrip(latLng: LatLng, address: String?): TripCreationResult
    suspend fun updateLocalOrder(orderId: String, updateFun: (Order) -> Unit)
    suspend fun completeTrip(tripId: String): SimpleResult
    suspend fun addOrderToTrip(tripId: String, orderParams: OrderCreationParams): Trip
}

class TripsRepositoryImpl(
    private val apiClient: ApiClient,
    private val tripsStorage: TripsStorage,
    private val coroutineScope: CoroutineScope,
    private val crashReportsProvider: CrashReportsProvider,
    private val orderAddressDelegate: OrderAddressDelegate
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

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun refreshTrips() {
        try {
            crashReportsProvider.log("Refresh trips")
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
            return TripCreationSuccess
        } catch (e: Exception) {
            return TripCreationError(e)
        }
    }

    override suspend fun completeTrip(tripId: String): SimpleResult {
        return apiClient.completeTrip(tripId)
    }

    override suspend fun updateLocalOrder(orderId: String, updateFun: (Order) -> Unit) {
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
                        it.orders
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
                                listOf()
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
            crashReportsProvider.logException(SimpleException("legacy trip received"))
            return listOf()
        } else {
            val localTrips = tripsStorage.getTrips().toMap { it.id }
            val newTrips = remoteTrips.map { remoteTrip ->
                if (remoteTrip.id in localTrips.keys) {
                    val localTrip = localTrips.getValue(remoteTrip.id!!)
                    val remoteOrders = (remoteTrip.orders ?: listOf())
                    val localOrders = localTrip.orders

                    val orders = localOrdersFromRemote(remoteOrders, localOrders)

                    return@map localTripFromRemote(remoteTrip, orders)
                } else {
                    localTripFromRemote(
                        remoteTrip,
                        localOrdersFromRemote(
                            remoteTrip.orders ?: listOf(),
                            listOf()
                        )
                    )
                }
            }
            return newTrips
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun localTripFromRemote(remoteTrip: Trip, newLocalOrders: List<Order>): LocalTrip {
        return LocalTrip(
            remoteTrip.id!!,
            TripStatus.fromString(remoteTrip.status),
            ((remoteTrip.metadata ?: mapOf<String, String>())
                .filter { it.value is String } as Map<String, String>)
                .toMutableMap(),
            newLocalOrders.toMutableList(),
            remoteTrip.views
        )
    }

    private suspend fun localOrdersFromRemote(
        remoteOrders: List<RemoteOrder>,
        oldLocalOrders: List<Order>,
    ): List<Order> {
        val localOrdersMap = oldLocalOrders.toMap { it.id }
        return remoteOrders.map {
            val localOrder = localOrdersMap[it.id]
            val res = createOrder(it, localOrder)
            res
        }
    }

    private suspend fun createOrder(remoteOrder: RemoteOrder, oldLocalOrder: Order?): Order {
        val remoteMetadata = Metadata.deserialize(remoteOrder.metadata)
        val localPhotosMap = oldLocalOrder?.photos?.toMap { it.photoId } ?: mapOf()
        val resPhotos = mutableSetOf<PhotoForUpload>().apply {
            addAll(oldLocalOrder?.photos ?: listOf())
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

        return Order.fromRemote(
            remoteOrder,
            note = oldLocalOrder?.note,
            photos = resPhotos,
            metadata = remoteMetadata,
            shortAddress = orderAddressDelegate.getShortAddress(remoteOrder),
            fullAddress = orderAddressDelegate.getFullAddress(remoteOrder),
        )
    }
}

sealed class TripCreationResult
object TripCreationSuccess : TripCreationResult()
class TripCreationError(val exception: Exception) : TripCreationResult()
