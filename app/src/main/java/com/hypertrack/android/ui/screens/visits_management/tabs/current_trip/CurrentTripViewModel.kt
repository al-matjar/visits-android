package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.interactors.TripsUpdateTimerInteractor
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.interactors.app.AppState
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersAdapter
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class CurrentTripViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appState: LiveData<AppState>,
    private val appCoroutineScope: CoroutineScope,
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val geocodingInteractor: GeocodingInteractor,
    private val tripsUpdateTimerInteractor: TripsUpdateTimerInteractor,
    private val locationProvider: DeviceLocationProvider,
    private val dateTimeFormatter: DateTimeFormatter,
    private val timeFormatter: TimeValueFormatter,
) : BaseViewModel(baseDependencies) {

    private val addressDelegate =
        OrderAddressDelegate(geocodingInteractor, osUtilsProvider, dateTimeFormatter)

    private val isTracking = MediatorLiveData<Boolean>().apply {
        addSource(appState) {
            it.isSdkTracking().let { isTracking ->
                if (value != isTracking) {
                    updateValue(isTracking)
                }
            }
        }
    }

    private val map = MutableLiveData<HypertrackMapWrapper>()

    init {
        map.observeManaged {
            displayUserLocation(it, userLocation.value)
        }
    }

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

    private var locationMarker: Marker? = null
    val userLocation: MediatorLiveData<LatLng?> = MediatorLiveData<LatLng?>().apply {
        postValue(null)
        addSource(locationProvider.deviceLocation) {
            if (map.value != null) {
                displayUserLocation(map.requireValue(), it)
            }
            postValue(it)
        }
    }

    private fun displayUserLocation(map: HypertrackMapWrapper, latLng: LatLng?) {
        locationMarker?.remove()
        locationMarker = map.addUserLocation(latLng)
    }

    val showWhereAreYouGoing: LiveData<Boolean> =
        ZipLiveData(appState, tripData).let {
            Transformations.map(it) { (appState, trip) ->
                return@map if (appState != null) {
                    trip == null && appState.isSdkTracking()
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
    }

    fun onViewCreated() {
        when (val state = appState.requireValue()) {
            is NotInitialized -> throw IllegalStateException(state.toString())
            is Initialized -> {
                if (state.tripCreationScope != null) {
                    proceedCreatingTrip(state.tripCreationScope.destinationData)
                } else {
                    if (loadingState.value != true) {
                        viewModelScope.launch {
                            tripsInteractor.refreshTrips()
                        }
                    }
                }
            }
        }
    }

    fun onMapReady(context: Context, googleMap: GoogleMap) {
        val mapWrapper = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = false,
                enableMyLocationButton = false,
                enableMyLocationIndicator = false
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
                destination.postValue(
                    VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                        geofenceId = it.value
                    )
                )
            }
        ) {
            override fun updateGeofencesOnMap(
                mapWrapper: HypertrackMapWrapper,
                geofences: List<Geofence>
            ) {
                if (tripData.value == null) {
                    super.updateGeofencesOnMap(mapWrapper, geofences)
                }
                displayUserLocation(mapWrapper, userLocation.value)
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

    private fun proceedCreatingTrip(destinationData: DestinationData) {
        loadingState.updateValue(true)
        appCoroutineScope.launch {
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
                title = osUtilsProvider.stringFromResource(R.string.share_trip_via)
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
            tripsInteractor.completeTrip(tripData.value!!.trip.id).let {
                when (it) {
                    JustSuccess -> {
                        map.value?.clear()
                    }
                    is JustFailure -> {
                        errorHandler.postException(it.exception)
                    }
                }
                loadingState.postValue(false)
            }
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
            map.requireValue().animateCamera(userLocation.value!!, DEFAULT_ZOOM)
        }
    }

    fun onTripFocused() {
        if (map.value != null && tripData.value != null) {
            map.requireValue().animateCameraToTrip(tripData.value!!.trip, userLocation.value)
        }
    }

    fun createOrdersAdapter(): OrdersAdapter {
        return OrdersAdapter(
            dateTimeFormatter,
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

    fun onResume() {
        tripsUpdateTimerInteractor.registerObserver(this.javaClass.simpleName)
    }

    fun onPause() {
        tripsUpdateTimerInteractor.unregisterObserver(this.javaClass.simpleName)
    }

    //todo fix map padding on first trip zoom
    private fun displayTripOnMap(map: HypertrackMapWrapper, it: LocalTrip?) {
        it?.let {
            map.clear()
            map.addTrip(it)
            map.animateCameraToTrip(it, userLocation.value)
        }
    }

    inner class TripData(val trip: LocalTrip) {
        val nextOrder = trip.nextOrder?.let { OrderData(it) }
        val ongoingOrders = trip.ongoingOrders
        val ongoingOrderText = osUtilsProvider
            .stringFromResource(R.string.you_have_ongoing_orders).let {
                val size = trip.ongoingOrders.size
                val plural = osUtilsProvider.getQuantityString(R.plurals.order, size)
                String.format(it, size, plural)
            }
    }

    inner class OrderData(val order: Order) {
        val address = order.shortAddress
            ?: resourceProvider.stringFromResource(R.string.address_not_available)
        val etaString = order.eta?.let { dateTimeFormatter.formatTime(it) }
            ?: osUtilsProvider.stringFromResource(R.string.orders_list_eta_unavailable)
        val etaAvailable = order.eta != null
        val awayText = order.awaySeconds?.let { seconds ->
            timeFormatter.formatSeconds(seconds.toInt())
        }
    }

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

}

