package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.interactors.TripsUpdateTimerInteractor
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.ui.common.MapParams
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.OrderAddressDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersAdapter
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.inflate_current_trip.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class CurrentTripViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val tripsUpdateTimerInteractor: TripsUpdateTimerInteractor,
    private val hyperTrackService: HyperTrackService,
    private val locationProvider: DeviceLocationProvider,
    private val datetimeFormatter: DatetimeFormatter,
    private val timeFormatter: TimeFormatter,
) : BaseViewModel(baseDependencies) {

    private val addressDelegate = OrderAddressDelegate(osUtilsProvider, datetimeFormatter)

    private val isTracking = MediatorLiveData<Boolean>().apply {
        addSource(hyperTrackService.isTracking) {
            if (value != it) {
                updateValue(it)
            }
        }
    }

    private val map = MutableLiveData<HypertrackMapWrapper>()

    private lateinit var geofencesMapDelegate: GeofencesMapDelegate

    override val errorHandler = ErrorHandler(
        osUtilsProvider,
        crashReportsProvider,
        exceptionSource = MediatorLiveData<Consumable<Exception>>().apply {
            addSource(tripsInteractor.errorFlow.asLiveData()) {
                postValue(it)
            }
            addSource(placesInteractor.errorFlow.asLiveData()) {
                postValue(it)
            }
        })

    val tripData = MediatorLiveData<TripData?>()
    val userLocation = MutableLiveData<LatLng?>()
    val showWhereAreYouGoing: LiveData<Boolean> =
        ZipLiveData(hyperTrackService.isTracking, tripData).let {
            Transformations.map(it) { (isTracking, trip) ->
                return@map if (isTracking != null) {
                    trip == null && isTracking
                } else {
                    false
                }
            }
        }
    val mapActiveState: LiveData<Boolean?> = ZipNotNullableLiveData(isTracking, map).let {
        Transformations.map(it) { (isTracking, map) ->
            isTracking
        }
    }

    init {
        tripData.addSource(tripsInteractor.currentTrip) { trip ->
            if (isTracking.requireValue()) {
                tripData.postValue(trip?.let { TripData(it) })
                map.value?.let { map -> displayTripOnMap(map, trip) }
            }
        }
        tripData.addSource(map) {
            if (isTracking.requireValue()) {
                displayTripOnMap(it, tripData.value?.trip)
            }
        }
        tripData.addSource(isTracking) {
            if (it) {
                map.value?.let {
                    tripsInteractor.currentTrip.value?.let { trip ->
                        this.tripData.postValue(TripData(trip))
                        displayTripOnMap(map.requireValue(), trip)
                    }
                }
            } else {
                tripData.postValue(null)
            }
        }

        mapActiveState.observeManaged {
            if (it != null && map.value != null) {
                val map = map.requireValue()
                //todo check geofences delegate
                if (it) {
                    val trip = tripsInteractor.currentTrip

                    if (trip.value != null) {
                        map.animateCameraToTrip(trip.value!!, userLocation.value)
                    } else {
                        if (userLocation.value != null) {
                            map.moveCamera(userLocation.value!!, DEFAULT_ZOOM)
                        }
                    }
                } else {
                    map.clear()
                    if (userLocation.value != null) {
                        map.moveCamera(userLocation.value!!, DEFAULT_ZOOM)
                    }
                }
            }
        }

        locationProvider.getCurrentLocation {
            userLocation.postValue(it?.toLatLng())
        }
    }

    fun onViewCreated() {
        if (loadingState.value != true) {
            viewModelScope.launch {
                tripsInteractor.refreshTrips()
            }
        }
    }

    fun onMapReady(context: Context, googleMap: GoogleMap) {
        val mapWrapper = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = false,
                enableMyLocationButton = false,
                enableMyLocationIndicator = true
            )
        ).apply {
            setOnCameraMovedListener {
                geofencesMapDelegate.onCameraIdle()
            }
        }

        geofencesMapDelegate = object : GeofencesMapDelegate(
            context,
            mapWrapper,
            placesInteractor,
            osUtilsProvider,
            {
                it.snippet.nullIfEmpty()?.let { snippet ->
                    destination.postValue(
                        VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                            snippet
                        )
                    )
                }
            }
        ) {
            override fun updateGeofencesOnMap(
                mapWrapper: HypertrackMapWrapper,
                geofences: List<LocalGeofence>
            ) {
                if (tripData.value == null) {
                    super.updateGeofencesOnMap(mapWrapper, geofences)
                }
            }

            override fun onCameraIdle() {
                if (tripData.value == null) {
                    super.onCameraIdle()
                }
            }
        }

        this.userLocation.observeManaged {
            if (tripData.value == null && it != null) {
                mapWrapper.moveCamera(it)
            }
        }

        map.postValue(mapWrapper)
    }

    fun onWhereAreYouGoingClick() {
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToSelectTripDestinationFragment()
        )
    }

    fun onDestinationResult(destinationData: DestinationData) {
        loadingState.updateValue(true)
        GlobalScope.launch {
            when (val res =
                tripsInteractor.createTrip(destinationData.latLng, destinationData.address)) {
                is TripCreationSuccess -> {
                }
                is TripCreationError -> {
                    errorHandler.postException(res.exception)
                }
            }
            loadingState.postValue(false)
        }
    }

    fun onShareTripClick() {
        tripData.value!!.trip.views?.shareUrl?.let {
            osUtilsProvider.shareText(
                text = it,
                title = osUtilsProvider.getString(R.string.share_trip_via)
            )
        }
    }

    fun onOrderClick(id: String) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToOrderDetailsFragment(
                id
            )
        )
    }

    fun onCompleteClick() {
        loadingState.postValue(true)
        viewModelScope.launch {
            tripsInteractor.completeTrip(tripData.value!!.trip.id)
            loadingState.postValue(false)
        }
    }

    fun onAddOrderClick() {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToAddOrderFragment(
                tripData.value!!.trip.id
            )
        )
    }

    fun onMyLocationClick() {
        if (map.value != null && userLocation.value != null) {
            map.requireValue().moveCamera(userLocation.value!!)
        }
    }

    fun createOrdersAdapter(): OrdersAdapter {
        return OrdersAdapter(
            datetimeFormatter,
            addressDelegate,
            showStatus = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (this::geofencesMapDelegate.isInitialized) {
            geofencesMapDelegate.onCleared()
        }
    }

    private fun displayTripOnMap(map: HypertrackMapWrapper, it: LocalTrip?) {
        it?.let {
            map.clear()
            map.addTrip(it)
            map.animateCameraToTrip(it, userLocation.value)
        }
    }

    private fun updateTripEta() {
        viewModelScope.launch {
            tripsInteractor.refreshTrips()
        }
    }

    fun onResume() {
        tripsUpdateTimerInteractor.registerObserver(this.javaClass.simpleName)
    }

    fun onPause() {
        tripsUpdateTimerInteractor.unregisterObserver(this.javaClass.simpleName)
    }

    inner class TripData(val trip: LocalTrip) {
        val isLegacy = trip.isLegacy()
        val nextOrder = trip.nextOrder?.let { OrderData(it) }
        val ongoingOrders = trip.ongoingOrders
        val ongoingOrderText = osUtilsProvider
            .stringFromResource(R.string.you_have_ongoing_orders).let {
                val size = trip.ongoingOrders.size
                val plural = osUtilsProvider.getQuantityString(R.plurals.order, size)
                String.format(it, size, plural)
            }
    }

    inner class OrderData(val order: LocalOrder) {
        val address = addressDelegate.shortAddress(order)
        val etaString = order.eta?.let { datetimeFormatter.formatTime(it) }
            ?: osUtilsProvider.stringFromResource(R.string.orders_list_eta_unavailable)
        val awayText = order.awaySeconds?.let { seconds ->
            timeFormatter.formatSeconds(seconds.toInt())
        }
    }

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

}

