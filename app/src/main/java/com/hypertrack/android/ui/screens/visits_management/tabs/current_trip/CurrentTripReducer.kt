package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import androidx.lifecycle.LiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.ui.common.map_state.TriggerLoadingGeofencesForMapPositionEffect
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.AddGeofencesMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateMapViewMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateUserLocationMapUiAction
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.ActiveTrip
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.MapOptic
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.InitializedState
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.MapUiStateOptic
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NoActiveTrip
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NotInitializedState
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.NotTracking
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.State
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.Tracking
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.ViewState
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.state.withEffects
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult

class CurrentTripReducer(
    private val appState: LiveData<AppState>,
    private val mapReducer: MapUiReducer,
    private val loadingState: LiveData<Boolean>,
    private val getViewState: (State) -> ViewState
) {

    fun reduce(state: State, action: Action): ReducerResult<out State, Effect> {
        return appState.requireValue().let { appState ->
            when (appState) {
                is AppInitialized -> {
                    when (appState.userState) {
                        is UserLoggedIn -> {
                            val userScope = appState.userState.userScope
                            reduceIfLoggedIn(
                                state,
                                action,
                                appState,
                                appState.userState,
                                userScope,
                                userScope.tripsInteractor.currentTrip.value,
                                userScope.deviceLocationProvider.deviceLocation.value,
                                loadingState.value ?: false
                            )
                        }
                        UserNotLoggedIn -> {
                            state.withEffects(ErrorEffect(IllegalActionException(action, appState)))
                        }
                    }
                }
                is AppNotInitialized -> {
                    state.withEffects(ErrorEffect(IllegalActionException(action, appState)))
                }
            }
        }
    }

    private fun reduceIfLoggedIn(
        state: State,
        action: Action,
        appState: AppInitialized,
        userState: UserLoggedIn,
        userScope: UserScope,
        trip: LocalTrip?,
        userLocation: LatLng?,
        isLoading: Boolean
    ): ReducerResult<out State, Effect> {
        return when (state) {
            is NotInitializedState -> {
                when (action) {
                    OnViewCreatedAction -> {
                        val newTrackingState = if (appState.isSdkTracking()) {
                            Tracking(
                                mapUiState = null,
                                userLocation = userLocation,
                                tripState = trip?.let {
                                    ActiveTrip(it)
                                } ?: NoActiveTrip
                            )
                        } else {
                            // todo show geofences in this state?
                            // map is initialized after OnViewCreatedAction
                            NotTracking(map = null)
                        }
                        val subscribeEffect = setOf(
                            SubscribeOnUserScopeEventsEffect(userScope)
                        )
                        val initialEffect = getInitialOnViewCreatedEffect(
                            appState.tripCreationScope,
                            isLoading,
                            userScope
                        )
                        val timerEffect = when (newTrackingState) {
                            is Tracking -> setOf(StartTripUpdateTimer(userScope))
                            is NotTracking -> setOf()
                        }
                        InitializedState(
                            trackingState = newTrackingState
                        ).withEffects(
                            subscribeEffect
                                    + initialEffect
                                    + timerEffect
                        )
                    }
                    is UserLocationChangedAction -> {
                        state.copy(userLocation = action.location).withEffects()
                    }
                    is ErrorAction -> {
                        reduce(action, state)
                    }
                    is OnMapReadyAction, is InitMapAction -> {
                        // action always comes after OnViewCreatedAction,
                        // and the state is changed on OnViewCreatedAction
                        state.withEffects(ErrorEffect(IllegalActionException(action, state)))
                    }
                    is TrackingStateChangedAction -> {
                        // no need to update the state, tracking state will be checked at
                        // NotInitialized -> Initialized
                        state.withEffects()
                    }
                    OnPauseAction,
                    OnResumeAction,
                    is GeofencesOnMapUpdatedAction,
                    is TripUpdatedAction -> {
                        // do nothing
                        state.withEffects()
                    }
                    OnAddOrderClickAction,
                    OnCompleteClickAction,
                    OnMyLocationClickAction,
                    OnShareTripClickAction,
                    OnTripFocusedAction,
                    OnWhereAreYouGoingClickAction,
                    is OnMarkerClickAction,
                    is MapUiAction,
                    is OnOrderClickAction -> {
                        // ui events are not possible here
                        state.withEffects(ErrorEffect(IllegalActionException(action, state)))
                    }
                }
            }
            is InitializedState -> {
                val map: HypertrackMapWrapper? = MapOptic.get(state)
                when (state.trackingState) {
                    is Tracking -> {
                        when (action) {
                            is OnViewCreatedAction -> {
                                reduce(
                                    action,
                                    state,
                                    appState,
                                    isLoading,
                                    userScope
                                )
                            }
                            is UserLocationChangedAction -> {
                                if (state.trackingState.mapUiState != null) {
                                    val newMapResult = mapReducer.reduce(
                                        UpdateUserLocationMapUiAction(action.location),
                                        state.trackingState.mapUiState
                                    )
                                    state.copy(
                                        trackingState = state.trackingState.copy(
                                            mapUiState = newMapResult.newState,
                                            userLocation = action.location
                                        )
                                    ).withEffects(
                                        newMapResult.effects.map { MapUiEffect(it) }.toSet()
                                    )
                                } else {
                                    state.copy(
                                        trackingState = state.trackingState.copy(
                                            userLocation = action.location
                                        )
                                    ).withEffects()
                                }
                            }
                            is ErrorAction -> {
                                reduce(action, state)
                            }
                            is TrackingStateChangedAction -> {
                                if (action.isTracking) {
                                    state.withEffects()
                                } else {
                                    //tracking stopped
                                    val timerEffect = setOf(
                                        StopTripUpdateTimer(userScope)
                                    )
                                    val mapEffects = if (map != null) {
                                        mutableSetOf<Effect>(
                                            SetMapActiveStateEffect(map, active = false)
                                        ).apply {
                                            if (userLocation != null) {
                                                add(
                                                    ClearAndMoveMapEffect(map, userLocation)
                                                )
                                            }
                                        }
                                    } else {
                                        setOf()
                                    }

                                    state.copy(
                                        trackingState = NotTracking(map)
                                    ).withEffects(timerEffect + mapEffects)
                                }
                            }
                            is OnAddOrderClickAction -> {
                                state.withEffects(
                                    if (trip != null) {
                                        NavigateEffect(
                                            VisitsManagementFragmentDirections
                                                .actionVisitManagementFragmentToAddOrderFragment(
                                                    trip.id
                                                )
                                        )
                                    } else {
                                        ErrorEffect(NullPointerException("trip is null"))
                                    }
                                )
                            }
                            is OnCompleteClickAction -> {
                                state.withEffects(
                                    trip?.let {
                                        setOf(CompleteTripEffect(it.id, map, userScope))
                                    } ?: setOf(
                                        ErrorEffect(NullPointerException("trip is null"))
                                    )
                                )
                            }
                            is OnMapReadyAction -> {
                                val newMap = action.map
                                mapReducer.reduce(
                                    UpdateMapViewMapUiAction(newMap), MapUiState(
                                        newMap,
                                        userLocation = userLocation
                                    )
                                ).withState {
                                    MapUiStateOptic.set(state, state.trackingState, it)
                                }.withEffects {
                                    val mapUiEffects = it.effects.map { MapUiEffect(it) }.toSet()
                                    val mapActiveStateEffect = setOf(
                                        SetMapActiveStateEffect(newMap, active = true)
                                    )
                                    val mapEffects =
                                        when (val tripState = state.trackingState.tripState) {
                                            is ActiveTrip -> {
                                                setOf(
                                                    AnimateMapToTripEffect(
                                                        newMap,
                                                        tripState.trip,
                                                        userLocation
                                                    )
                                                )
                                            }
                                            is NoActiveTrip -> {
                                                val userLocationEffect = if (userLocation != null) {
                                                    setOf(
                                                        MoveMapEffect(newMap, userLocation)
                                                    )
                                                } else setOf()

                                                val geofencesOnMapEffect = MapUiEffect(
                                                    TriggerLoadingGeofencesForMapPositionEffect(
                                                        newMap
                                                    )
                                                )
                                                userLocationEffect + geofencesOnMapEffect
                                            }
                                        }
                                    mapActiveStateEffect + mapEffects + mapUiEffects
                                }
                            }
                            OnMyLocationClickAction -> {
                                state.withEffects(
                                    if (map != null && userLocation != null) {
                                        setOf(AnimateMapEffect(map, userLocation))
                                    } else {
                                        setOf()
                                    }
                                )
                            }
                            is OnOrderClickAction -> {
                                state.withEffects(
                                    NavigateEffect(
                                        VisitsManagementFragmentDirections
                                            .actionVisitManagementFragmentToOrderDetailsFragment(
                                                action.orderId
                                            )
                                    )
                                )
                            }
                            is OnResumeAction -> {
                                state.withEffects(
                                    StartTripUpdateTimer(userScope)
                                )
                            }
                            is OnPauseAction -> {
                                state.withEffects(
                                    StopTripUpdateTimer(userScope)
                                )
                            }
                            is OnShareTripClickAction -> {
                                reduce(action, state, trip)
                            }
                            OnTripFocusedAction -> {
                                state.withEffects(
                                    if (trip != null) {
                                        if (map != null) {
                                            setOf(
                                                AnimateMapToTripEffect(
                                                    map,
                                                    trip,
                                                    userLocation
                                                )
                                            )
                                        } else {
                                            setOf()
                                        }
                                    } else {
                                        setOf(ErrorEffect(NullPointerException("trip is null")))
                                    }
                                )
                            }
                            is OnWhereAreYouGoingClickAction -> {
                                reduce(action, state)
                            }
                            is TripUpdatedAction -> {
                                // either trip data is updated, or trip added/removed
                                val newTripState = action.trip?.let {
                                    ActiveTrip(it)
                                } ?: NoActiveTrip

                                if (map != null) {
                                    val newMapResult = mapReducer.reduce(
                                        UpdateMapViewMapUiAction(map),
                                        MapUiState(
                                            map,
                                            userLocation = userLocation,
                                            trip = action.trip,
                                            geofences = setOf()
                                        )
                                    )
                                    newMapResult.withState {
                                        state.copy(
                                            trackingState = state.trackingState.copy(
                                                tripState = newTripState,
                                                mapUiState = it
                                            )
                                        )
                                    }.withEffects {
                                        val mapUiEffects =
                                            it.effects.map { MapUiEffect(it) }.toSet()

                                        val mapEffects = when (newTripState) {
                                            is ActiveTrip -> {
                                                // if new trip added or new order added
                                                val oldTripState = state.trackingState.tripState
                                                if (
                                                    oldTripState is NoActiveTrip ||
                                                    (oldTripState is ActiveTrip &&
                                                            oldTripState.trip.orders != newTripState.trip.orders)
                                                ) {
                                                    setOf(
                                                        AnimateMapToTripEffect(
                                                            map,
                                                            newTripState.trip,
                                                            userLocation
                                                        )
                                                    )
                                                } else setOf()
                                            }
                                            // tracking started, and there is no trip
                                            is NoActiveTrip -> {
                                                val userLocationEffects =
                                                    if (userLocation != null) {
                                                        setOf(
                                                            MoveMapEffect(map, userLocation)
                                                        )
                                                    } else setOf()
                                                val geofencesOnMapEffects = MapUiEffect(
                                                    TriggerLoadingGeofencesForMapPositionEffect(
                                                        map
                                                    )
                                                )
                                                userLocationEffects + geofencesOnMapEffects
                                            }
                                        }
                                        mapUiEffects + mapEffects
                                    }
                                } else {
                                    state.copy(
                                        trackingState = state.trackingState.copy(
                                            tripState = newTripState
                                        )
                                    ).withEffects()
                                }
                            }
                            is InitMapAction -> {
                                reduce(action, state, userScope)
                            }
                            is MapUiAction -> {
                                if (
                                    state.trackingState.tripState is NoActiveTrip &&
                                    state.trackingState.mapUiState != null
                                ) {
                                    mapReducer.reduce(
                                        action.action,
                                        state.trackingState.mapUiState
                                    ).withState {
                                        state.copy(
                                            trackingState = state.trackingState.copy(
                                                mapUiState = it
                                            )
                                        )
                                    }.withEffects {
                                        it.effects.map { effect -> MapUiEffect(effect) }.toSet()
                                    }
                                } else state.withEffects()
                            }
                            is OnMarkerClickAction -> {
                                state.withEffects(
                                    NavigateEffect(
                                        VisitsManagementFragmentDirections.actionGlobalPlaceDetailsFragment(
                                            action.snippet
                                        )
                                    )
                                )
                            }
                            is GeofencesOnMapUpdatedAction -> {
                                when (state.trackingState.tripState) {
                                    is ActiveTrip -> {
                                        state.withEffects()
                                    }
                                    NoActiveTrip -> {
                                        if (state.trackingState.mapUiState != null) {
                                            mapReducer.reduce(
                                                AddGeofencesMapUiAction(action.geofences),
                                                state.trackingState.mapUiState
                                            ).withState {
                                                state.copy(
                                                    trackingState = state.trackingState.copy(
                                                        mapUiState = it
                                                    )
                                                )
                                            }.withEffects { result ->
                                                result.effects.map { MapUiEffect(it) }.toSet()
                                            }
                                        } else {
                                            state.withEffects()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is NotTracking -> {
                        when (action) {
                            is ErrorAction -> {
                                reduce(action, state)
                            }
                            is OnMapReadyAction -> {
                                state.copy(
                                    trackingState = state.trackingState.copy(map = action.map)
                                ).withEffects(
                                    setOf(
                                        SetMapActiveStateEffect(action.map, active = false)
                                    ) + if (userLocation != null) {
                                        setOf(MoveMapEffect(action.map, userLocation))
                                    } else {
                                        setOf()
                                    }
                                )
                            }
                            is OnShareTripClickAction -> {
                                reduce(action, state, trip)
                            }
                            is OnViewCreatedAction -> {
                                reduce(
                                    action,
                                    state,
                                    appState,
                                    isLoading,
                                    userScope
                                )
                            }
                            is OnWhereAreYouGoingClickAction -> {
                                reduce(action, state)
                            }
                            is TrackingStateChangedAction -> {
                                if (action.isTracking) {
                                    // tracking started
                                    val map = MapOptic.get(state)

                                    val newTripState = trip?.let {
                                        ActiveTrip(it)
                                    } ?: NoActiveTrip

                                    if (map != null) {
                                        mapReducer.reduce(
                                            UpdateMapViewMapUiAction(map),
                                            MapUiState(
                                                map,
                                                userLocation = userLocation,
                                                trip = trip,
                                                geofences = setOf()
                                            )
                                        ).withState {
                                            state.copy(
                                                trackingState = Tracking(
                                                    tripState = newTripState,
                                                    userLocation = userLocation,
                                                    mapUiState = it
                                                )
                                            )
                                        }.withEffects {
                                            val mapUiEffects =
                                                it.effects.map { MapUiEffect(it) }.toSet()
                                            val timerEffect = setOf(
                                                StartTripUpdateTimer(userScope)
                                            )
                                            val mapEffects = setOf(
                                                SetMapActiveStateEffect(map, active = true),
                                            ) + when (newTripState) {
                                                is ActiveTrip -> {
                                                    setOf(
                                                        AnimateMapToTripEffect(
                                                            map,
                                                            newTripState.trip,
                                                            userLocation
                                                        )
                                                    )
                                                }
                                                // tracking started, and there is no trip
                                                is NoActiveTrip -> {
                                                    val userLocationEffects =
                                                        if (userLocation != null) {
                                                            setOf(
                                                                MoveMapEffect(map, userLocation)
                                                            )
                                                        } else setOf()
                                                    val geofencesOnMapEffects = MapUiEffect(
                                                        TriggerLoadingGeofencesForMapPositionEffect(
                                                            map
                                                        )
                                                    )
                                                    userLocationEffects + geofencesOnMapEffects
                                                }
                                            }
                                            mapUiEffects + timerEffect + mapEffects
                                        }
                                    } else {
                                        state.copy(
                                            trackingState = Tracking(
                                                tripState = newTripState,
                                                userLocation = userLocation,
                                                mapUiState = null
                                            )
                                        ).withEffects()
                                    }
                                } else {
                                    state.withEffects()
                                }
                            }
                            is InitMapAction -> {
                                reduce(action, state, userScope)
                            }
                            OnResumeAction,
                            OnPauseAction,
                            is MapUiAction,
                            is TripUpdatedAction,
                            is GeofencesOnMapUpdatedAction,
                            is UserLocationChangedAction -> {
                                //do nothing
                                state.withEffects()
                            }
                            OnAddOrderClickAction,
                            OnCompleteClickAction,
                            OnMyLocationClickAction,
                            OnTripFocusedAction,
                            is OnMarkerClickAction,
                            is OnOrderClickAction,
                            -> {
                                // illegal actions
                                state.withEffects(
                                    ErrorEffect(IllegalActionException(action, state))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun stateChangeEffects(state: State): Set<Effect> {
        return setOf(
            UpdateViewStateEffect(getViewState(state))
        )
    }

    private fun reduce(action: ErrorAction, state: State): ReducerResult<State, Effect> {
        return state.withEffects(ErrorEffect(action.exception))
    }

    private fun reduce(
        action: OnViewCreatedAction,
        state: InitializedState,
        appState: AppInitialized,
        isLoading: Boolean,
        userScope: UserScope
    ): ReducerResult<State, Effect> {
        return state.withEffects(
            getInitialOnViewCreatedEffect(
                appState.tripCreationScope,
                isLoading,
                userScope
            )
        )
    }

    private fun reduce(
        action: InitMapAction,
        state: State,
        userScope: UserScope
    ): ReducerResult<State, Effect> {
        return state.withEffects(PrepareMapEffect(action.context, action.map, userScope))
    }

    private fun reduce(
        action: OnWhereAreYouGoingClickAction,
        state: State
    ): ReducerResult<State, Effect> {
        return state.withEffects(
            NavigateEffect(
                VisitsManagementFragmentDirections
                    .actionVisitManagementFragmentToSelectTripDestinationFragment()
            )
        )
    }

    private fun reduce(
        action: OnShareTripClickAction,
        state: State,
        trip: LocalTrip?
    ): ReducerResult<State, Effect> {
        return state.withEffects(
            trip?.views?.shareUrl?.let {
                setOf(ShareTripLinkEffect(it))
            } ?: setOf()
        )
    }

    private fun getInitialOnViewCreatedEffect(
        tripCreationScope: TripCreationScope?,
        isLoading: Boolean,
        userScope: UserScope
    ): Set<Effect> {
        return if (tripCreationScope != null) {
            // there is pending trip creation request
            setOf(
                ProceedTripCreationEffect(
                    tripCreationScope.destinationData,
                    userScope
                )
            )
        } else {
            if (!isLoading) {
                setOf(RefreshTripsEffect(userScope))
            } else {
                setOf()
            }
        }
    }

}
