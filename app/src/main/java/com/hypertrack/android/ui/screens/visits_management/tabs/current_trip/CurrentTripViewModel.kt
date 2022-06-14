package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.DestroyTripCreationScopeAction
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersAdapter
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.catchException
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("OPT_IN_USAGE")
class CurrentTripViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
) : BaseViewModel(baseDependencies) {

    private val reducer = CurrentTripReducer(
        appInteractor.appState,
        loadingState,
        this::getViewState
    )
    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        NotInitializedState(
            userLocation = null
        ),
        viewModelScope,
        appInteractor.appScope.stateMachineContext,
        reducer::reduce,
        this::applyEffects,
        reducer::stateChangeEffects
    )

    private val addressDelegate =
        OrderAddressDelegate(
            appInteractor.appScope.geocodingInteractor,
            osUtilsProvider,
            appInteractor.appScope.dateTimeFormatter
        )
    private val mapStyleActive by lazy {
        MapStyleOptions.loadRawResourceStyle(
            appInteractor.appScope.appContext,
            R.raw.style_map
        )
    }
    private val mapStyleInactive by lazy {
        MapStyleOptions.loadRawResourceStyle(
            appInteractor.appScope.appContext,
            R.raw.style_map_silver
        )
    }

    private lateinit var geofencesMapDelegate: GeofencesMapDelegate
    private var locationMarker: Marker? = null

    val viewState = MutableLiveData<ViewState>()

    init {
        appInteractor.appState.observeManaged { appState ->
            when (appState) {
                is Initialized -> {
                    when (appState.userState) {
                        is UserLoggedIn -> {
                            handleAction(TrackingStateChangedAction(appState.isSdkTracking()))
                        }
                        UserNotLoggedIn -> {
                        }
                    }
                }
                is NotInitialized -> {
                }
            }
        }
    }

    fun handleAction(action: Action) {
        stateMachine.handleAction(action)
    }

    fun createOrdersAdapter(): OrdersAdapter {
        return OrdersAdapter(
            appInteractor.appScope.dateTimeFormatter,
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

    private fun applyEffects(effects: Set<Effect>) {
        for (effect in effects) {
            applyEffect(effect)
        }
    }

    private fun applyEffect(effect: Effect) {
        appInteractor.appScope.appCoroutineScope.launch {
            getEffectFlow(effect)
                .catchException {
//                    crashReportsProvider.logException(it)
                    showExceptionMessageAndReport(it)
                }.collect()
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<Unit> {
        return when (effect) {
            is ErrorEffect -> {
                getErrorFlow(effect.exception)
            }
            is UpdateViewStateEffect -> {
                { viewState.postValue(effect.viewState) }.asFlow()
            }
            is SubscribeOnUserScopeEventsEffect -> {
                getSubscribeOnUserScopeEventsFlow(effect.userScope)
            }
            is AddUserLocationToMap -> {
                {
                    locationMarker?.remove()
                    locationMarker = effect.map.addUserLocation(effect.location)
                }.asFlow().flowOn(Dispatchers.Main)
            }
            is ClearAndDisplayTripAndUserLocationOnMapEffect -> {
                {
                    effect.map.clear()
                    effect.map.addTrip(effect.trip)
                    effect.map.addUserLocation(effect.userLocation)
                    effect.map.animateCameraToTrip(effect.trip, effect.userLocation)
                }.asFlow().flowOn(Dispatchers.Main)
            }
            is ClearMapEffect -> {
                { effect.map.clear() }.asFlow().flowOn(Dispatchers.Main)
            }
            is MoveMapEffect -> {
                { effect.map.moveCamera(effect.position, DEFAULT_ZOOM) }.asFlow()
                    .flowOn(Dispatchers.Main)
            }
            is ClearAndMoveMapEffect -> {
                {
                    effect.map.clear()
                    effect.map.moveCamera(effect.position, DEFAULT_ZOOM)
                }.asFlow().flowOn(Dispatchers.Main)
            }
            is ClearMapAndDisplayUserLocationEffect -> {
                {
                    effect.map.clear()
                    effect.map.addUserLocation(effect.userLocation)
                    Unit
                }.asFlow().flowOn(Dispatchers.Main)
            }
            is PrepareMapEffect -> {
                getPrepareMapFlow(effect.context, effect.map, effect.userScope)
            }
            is AnimateMapToTripEffect -> {
                //todo fix map padding on first trip zoom
                { effect.map.animateCameraToTrip(effect.trip, effect.userLocation) }.asFlow()
                    .flowOn(Dispatchers.Main)
            }
            is AnimateMapEffect -> {
                { effect.map.animateCamera(effect.position, DEFAULT_ZOOM) }.asFlow()
                    .flowOn(Dispatchers.Main)
            }
            is SetMapActiveState -> {
                {
                    effect.map.googleMap.setMapStyle(
                        if (effect.active) {
                            mapStyleActive
                        } else {
                            mapStyleInactive
                        }
                    )
                    Unit
                }.asFlow().flowOn(Dispatchers.Main)
            }
            is ProceedTripCreationEffect -> {
                getProceedCreatingTripFlow(
                    effect.userScope,
                    effect.destinationData
                )
            }
            is RefreshTripsEffect -> {
                suspend { effect.userScope.tripsInteractor.refreshTrips() }.asFlow()
            }
            is NavigateEffect -> {
                { destination.postValue(effect.destination) }.asFlow()
            }
            is CompleteTripEffect -> {
                getOnCompleteClickFlow(effect.tripId, effect.map, effect.userScope)
            }
            is ShareTripLinkEffect -> {
                {
                    effect.url.let {
                        osUtilsProvider.shareText(
                            text = it,
                            title = osUtilsProvider.stringFromResource(R.string.share_trip_via)
                        )
                    }
                }.asFlow()
            }
            is StartTripUpdateTimer -> {
                {
                    effect.userScope.tripsUpdateTimerInteractor
                        .registerObserver(this.javaClass.simpleName)
                }.asFlow()
            }
            is StopTripUpdateTimer -> {
                {
                    effect.userScope.tripsUpdateTimerInteractor
                        .unregisterObserver(this.javaClass.simpleName)
                }.asFlow()
            }
        }
    }

    private fun getErrorFlow(exception: Exception): Flow<Unit> {
        return { showExceptionMessageAndReport(exception) }.asFlow()
    }

    private fun getSubscribeOnUserScopeEventsFlow(userScope: UserScope): Flow<Unit> {
        return {
            val observer = { e: Consumable<Exception> ->
                e.consume {
                    handleAction(ErrorAction(it))
                }
            }
            userScope.tripsInteractor.errorFlow.asLiveData().observeManaged(observer)
            userScope.placesInteractor.errorFlow.asLiveData().observeManaged(observer)

            userScope.deviceLocationProvider.deviceLocation.observeManaged {
                handleAction(UserLocationChangedAction(it))
            }

            userScope.tripsInteractor.currentTrip.observeManaged { trip ->
                handleAction(TripUpdatedAction(trip))
            }
        }.asFlow().flowOn(Dispatchers.Main)
    }

    private fun getPrepareMapFlow(
        context: Context,
        googleMap: GoogleMap,
        userScope: UserScope
    ): Flow<Unit> {
        return {
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
                userScope.placesInteractor,
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
                    if (userScope.tripsInteractor.currentTrip.value == null) {
                        super.updateGeofencesOnMap(mapWrapper, geofences)
                    }
                    // the update clears the map, so we need to add user location back
                    handleAction(OnGeofencesUpdateAction)
                }

                override fun onCameraIdle() {
                    if (userScope.tripsInteractor.currentTrip.value == null) {
                        super.onCameraIdle()
                    }
                }
            }
            mapWrapper
        }.asFlow()
            .map {
                handleAction(OnMapReadyAction(context, it))
                Unit
            }
            .flowOn(Dispatchers.Main)
    }

    private fun getProceedCreatingTripFlow(
        userScope: UserScope,
        destinationData: DestinationData
    ): Flow<Unit> {
        return suspend {
            appInteractor.handleAction(DestroyTripCreationScopeAction)
            loadingState.updateValue(true)
            userScope.tripsInteractor.createTrip(
                destinationData.latLng,
                destinationData.address
            ).let { res ->
                when (res) {
                    is TripCreationSuccess -> {
                    }
                    is TripCreationError -> {
                        showExceptionMessageAndReport(res.exception)
                    }
                }
            }
            loadingState.postValue(false)
        }.asFlow().flowOn(Dispatchers.Main)
    }

    private fun getOnCompleteClickFlow(
        tripId: String,
        map: HypertrackMapWrapper?,
        userScope: UserScope
    ): Flow<Unit> {
        return suspend {
            loadingState.postValue(true)
            userScope.tripsInteractor.completeTrip(tripId)
        }.asFlow()
            .flowOn(appInteractor.appScope.stateMachineContext)
            .map {
                when (it) {
                    JustSuccess -> map?.clear()
                    is JustFailure -> showExceptionMessageAndReport(it.exception)
                }
                loadingState.postValue(false)
            }
            .flowOn(Dispatchers.Main)
    }

    private fun getViewState(state: State): ViewState {
        return when (state) {
            is NotInitializedState -> {
                ViewState(
                    showWhereAreYouGoingButton = false,
                    tripData = null,
                    userLocation = null
                )
            }
            is InitializedState -> {
                when (state.trackingState) {
                    NotTracking -> {
                        ViewState(
                            showWhereAreYouGoingButton = false,
                            tripData = null,
                            userLocation = null
                        )
                    }
                    is Tracking -> {
                        val trip = state.trackingState.trip
                        ViewState(
                            showWhereAreYouGoingButton = trip == null,
                            tripData = trip?.let {
                                TripData(
                                    nextOrder = trip.nextOrder?.let { order ->
                                        OrderData(
                                            address = order.shortAddress
                                                ?: resourceProvider.stringFromResource(R.string.address_not_available),
                                            etaString = order.eta?.let {
                                                appInteractor.appScope.dateTimeFormatter.formatTime(
                                                    it
                                                )
                                            }
                                                ?: osUtilsProvider.stringFromResource(R.string.orders_list_eta_unavailable),
                                            etaAvailable = order.eta != null,
                                            awayText = order.awaySeconds?.let { seconds ->
                                                appInteractor.appScope.timeFormatter.formatSeconds(
                                                    seconds.toInt()
                                                )
                                            }
                                        )
                                    },
                                    ongoingOrders = trip.ongoingOrders,
                                    ongoingOrderText = osUtilsProvider
                                        .stringFromResource(R.string.you_have_ongoing_orders).let {
                                            val size = trip.ongoingOrders.size
                                            val plural = osUtilsProvider.getQuantityString(
                                                R.plurals.order,
                                                size
                                            )
                                            String.format(it, size, plural)
                                        }
                                )
                            },
                            userLocation = state.trackingState.userLocation
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

}

