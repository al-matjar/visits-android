package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.DestroyTripCreationScopeAction
import com.hypertrack.android.interactors.app.GeofencesForMapUpdatedEvent
import com.hypertrack.android.interactors.app.TrackingStateChangedEvent
import com.hypertrack.android.interactors.app.noAction
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.map_state.OnMapMovedMapUiAction
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.ActiveTrip
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.InitializedState
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NoActiveTrip
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NotInitializedState
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NotTracking
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.State
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.Tracking
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.ViewState
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

@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class CurrentTripViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val mapUiEffectHandler: MapUiEffectHandler
) : BaseViewModel(baseDependencies) {

    private val reducer = CurrentTripReducer(
        appInteractor.appState,
        MapUiReducer(),
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

    val viewState = MutableLiveData<ViewState>()

    init {
        runInVmEffectsScope {
            appInteractor.appEvent.collect {
                when (it) {
                    is GeofencesForMapUpdatedEvent -> {
                        handleAction(GeofencesForMapUpdatedAction(it.geofences))
                    }
                    is TrackingStateChangedEvent -> {
                        handleAction(TrackingStateChangedAction(it.trackingState.isTracking()))
                    }
                    else -> {
                    }
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

    private fun applyEffects(effects: Set<Effect>) {
        for (effect in effects) {
            applyEffect(effect)
        }
    }

    private fun applyEffect(effect: Effect) {
        appInteractor.appScope.appCoroutineScope.launch {
            getEffectFlow(effect)
                .catchException {
                    showExceptionMessageAndReport(it)
                }.collect {
                    it?.let { handleAction(it) }
                }
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<Action?> {
        return when (effect) {
            is ErrorEffect -> {
                getErrorFlow(effect.exception).noAction()
            }
            is UpdateViewStateEffect -> {
                { viewState.postValue(effect.viewState) }.asFlow().noAction()
            }
            is SubscribeOnUserScopeEventsEffect -> {
                getSubscribeOnUserScopeEventsFlow(effect.userScope).noAction()
            }
            is ClearMapEffect -> {
                { effect.map.clear() }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is MoveMapEffect -> {
                { effect.map.moveCamera(effect.position, DEFAULT_ZOOM) }.asFlow()
                    .flowOn(Dispatchers.Main).noAction()
            }
            is ClearAndMoveMapEffect -> {
                {
                    effect.map.clear()
                    effect.map.moveCamera(effect.position, DEFAULT_ZOOM)
                }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is PrepareMapEffect -> {
                getPrepareMapFlow(effect.context, effect.map).noAction()
            }
            is AnimateMapToTripEffect -> {
                //todo fix map padding on first trip zoom
                { effect.map.animateCameraToTrip(effect.trip, effect.userLocation) }.asFlow()
                    .flowOn(Dispatchers.Main).noAction()
            }
            is AnimateMapEffect -> {
                { effect.map.animateCamera(effect.position, DEFAULT_ZOOM) }.asFlow()
                    .flowOn(Dispatchers.Main).noAction()
            }
            is SetMapActiveStateEffect -> {
                {
                    effect.map.googleMap.setMapStyle(
                        if (effect.active) {
                            mapStyleActive
                        } else {
                            mapStyleInactive
                        }
                    )
                    Unit
                }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is ProceedTripCreationEffect -> {
                getProceedCreatingTripFlow(
                    effect.userScope,
                    effect.destinationData
                ).noAction()
            }
            is RefreshTripsEffect -> {
                suspend { effect.userScope.tripsInteractor.refreshTrips() }.asFlow().noAction()
            }
            is NavigateEffect -> {
                { destination.postValue(effect.destination) }.asFlow().noAction()
            }
            is CompleteTripEffect -> {
                getOnCompleteClickFlow(effect.tripId, effect.map, effect.userScope).noAction()
            }
            is ShareTripLinkEffect -> {
                {
                    effect.url.let {
                        osUtilsProvider.shareText(
                            text = it,
                            title = osUtilsProvider.stringFromResource(R.string.share_trip_via)
                        )
                    }
                }.asFlow().noAction()
            }
            is StartTripUpdateTimer -> {
                {
                    effect.userScope.tripsUpdateTimerInteractor
                        .registerObserver(this.javaClass.simpleName)
                }.asFlow().noAction()
            }
            is StopTripUpdateTimer -> {
                {
                    effect.userScope.tripsUpdateTimerInteractor
                        .unregisterObserver(this.javaClass.simpleName)
                }.asFlow().noAction()
            }
            is MapUiEffect -> {
                mapUiEffectHandler.getEffectFlow(effect.effect).map { it?.let { MapUiAction(it) } }
            }
            is SetMapPaddingEffect -> {
                { effect.map.setPadding(bottom = effect.bottomPadding) }.asFlow()
                    .flowOn(Dispatchers.Main).noAction()
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
    ): Flow<Unit> {
        return {
            HypertrackMapWrapper(
                googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                    enableScroll = true,
                    enableZoomKeys = false,
                    enableMyLocationButton = false,
                    enableMyLocationIndicator = false
                )
            ).apply {
                setOnCameraMovedListener {
                    handleAction(MapUiAction(OnMapMovedMapUiAction(it)))
                }

                setOnMarkerClickListener { marker ->
                    marker.snippet?.let {
                        handleAction(OnMarkerClickAction(it))
                    }
                }
            }
        }.asFlow()
            .map {
                handleAction(OnMapReadyAction(context, it))
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
                        // trip polyline is created with some delay, so we need to update the trip
                        // to get the polyline
                        userScope.tripsInteractor.refreshTrips()
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
                    is NotTracking -> {
                        ViewState(
                            showWhereAreYouGoingButton = false,
                            tripData = null,
                            userLocation = null
                        )
                    }
                    is Tracking -> {
                        val trip = when (val tripState = state.trackingState.tripState) {
                            is ActiveTrip -> tripState.trip
                            is NoActiveTrip -> null
                        }
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

