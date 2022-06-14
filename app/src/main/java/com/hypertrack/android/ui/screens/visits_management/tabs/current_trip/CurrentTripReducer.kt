package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import androidx.lifecycle.LiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.TripCreationScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppState
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.ReducerResult

class CurrentTripReducer(
    private val appState: LiveData<AppState>,
    private val loadingState: LiveData<Boolean>,
    private val getViewState: (State) -> ViewState
) {

    fun reduce(state: State, action: Action): ReducerResult<State, Effect> {
        return appState.requireValue().let { appState ->
            when (appState) {
                is Initialized -> {
                    when (appState.userState) {
                        is UserLoggedIn -> {
                            val userScope = appState.userState.userScope
                            reduceIfLoggedIn(
                                state,
                                action,
                                appState,
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
                is NotInitialized -> {
                    state.withEffects(ErrorEffect(IllegalActionException(action, appState)))
                }
            }
        }
    }

    private fun reduceIfLoggedIn(
        state: State,
        action: Action,
        appState: Initialized,
        userScope: UserScope,
        trip: LocalTrip?,
        userLocation: LatLng?,
        isLoading: Boolean
    ): ReducerResult<State, Effect> {
        return when (state) {
            is NotInitializedState -> {
                when (action) {
                    OnViewCreatedAction -> {
                        val newTrackingState = if (appState.isSdkTracking()) {
                            Tracking(userLocation, trip)
                        } else {
                            NotTracking
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
                            NotTracking -> setOf()
                        }
                        InitializedState(
                            // map is initialized after OnViewCreatedAction
                            map = null,
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
                    OnGeofencesUpdateAction,
                    is TripUpdatedAction -> {
                        // do nothing
                        state.withEffects()
                    }
                    OnAddOrderClickAction,
                    OnCompleteClickAction,
                    OnMyLocationClickAction,
                    is OnOrderClickAction,
                    OnShareTripClickAction,
                    OnTripFocusedAction,
                    OnWhereAreYouGoingClickAction -> {
                        // ui events are not possible here
                        state.withEffects(ErrorEffect(IllegalActionException(action, state)))
                    }
                }
            }
            is InitializedState -> {
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
                                state.copy(
                                    trackingState = state.trackingState.copy(
                                        userLocation = action.location
                                    )
                                ).withEffects(
                                    if (state.map != null) {
                                        setOf(AddUserLocationToMap(state.map, action.location))
                                    } else {
                                        setOf()
                                    }
                                )
                            }
                            is ErrorAction -> {
                                reduce(action, state)
                            }
                            is TrackingStateChangedAction -> {
                                if (action.isTracking) {
                                    state.withEffects()
                                } else {
                                    //tracking stopped
                                    val effects = if (state.map != null) {
                                        mutableSetOf<Effect>(
                                            SetMapActiveState(state.map, active = false)
                                        ).apply {
                                            if (userLocation != null) {
                                                add(
                                                    ClearAndMoveMapEffect(state.map, userLocation)
                                                )
                                            }
                                        }
                                    } else {
                                        setOf()
                                    } + setOf(
                                        StopTripUpdateTimer(userScope)
                                    )
                                    state.copy(
                                        trackingState = NotTracking
                                    ).withEffects(effects)
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
                                        setOf(CompleteTripEffect(it.id, state.map, userScope))
                                    } ?: setOf(
                                        ErrorEffect(NullPointerException("trip is null"))
                                    )
                                )
                            }
                            is OnMapReadyAction -> {
                                val map = action.map
                                val mapStateEffects = setOf(
                                    SetMapActiveState(map, active = true)
                                ) + if (userLocation != null) {
                                    setOf(
                                        AddUserLocationToMap(map, userLocation)
                                    )
                                } else {
                                    setOf()
                                }
                                val effects = mapStateEffects + if (trip != null) {
                                    setOf(
                                        AnimateMapToTripEffect(map, trip, userLocation)
                                    )
                                } else {
                                    if (userLocation != null) {
                                        setOf(
                                            MoveMapEffect(map, userLocation)
                                        )
                                    } else {
                                        setOf()
                                    }
                                }
                                state.copy(map = map).withEffects(effects)
                            }
                            OnMyLocationClickAction -> {
                                state.withEffects(
                                    if (state.map != null && userLocation != null) {
                                        setOf(AnimateMapEffect(state.map, userLocation))
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
                                        if (state.map != null) {
                                            setOf(
                                                AnimateMapToTripEffect(
                                                    state.map,
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
                                state.copy(
                                    trackingState = state.trackingState.copy(
                                        trip = action.trip
                                    )
                                ).withEffects(
                                    if (state.map != null) {
                                        if (action.trip != null) {
                                            setOf(
                                                ClearAndDisplayTripAndUserLocationOnMapEffect(
                                                    state.map,
                                                    action.trip,
                                                    userLocation
                                                )
                                            )
                                        } else {
                                            setOf(
                                                ClearMapAndDisplayUserLocationEffect(
                                                    state.map,
                                                    state.trackingState.userLocation
                                                )
                                            )
                                        }
                                    } else {
                                        setOf()
                                    }
                                )
                            }
                            is InitMapAction -> {
                                reduce(action, state, userScope)
                            }
                            OnGeofencesUpdateAction -> {
                                state.withEffects(
                                    if (state.map != null) {
                                        setOf(
                                            AddUserLocationToMap(
                                                state.map,
                                                userScope.deviceLocationProvider.deviceLocation.value
                                            )
                                        )
                                    } else setOf()
                                )
                            }
                        }
                    }
                    NotTracking -> {
                        when (action) {
                            is ErrorAction -> {
                                reduce(action, state)
                            }
                            is OnMapReadyAction -> {
                                state.copy(map = action.map).withEffects(
                                    setOf(
                                        SetMapActiveState(action.map, active = false)
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
                                    val newState = state.copy(
                                        trackingState = Tracking(
                                            trip = trip,
                                            userLocation = userLocation
                                        )
                                    )
                                    val map = state.map
                                    val effects = setOf(
                                        StartTripUpdateTimer(userScope)
                                    ) + if (map != null) {
                                        setOf(
                                            SetMapActiveState(map, active = true),
                                        ) + when {
                                            trip != null -> {
                                                setOf(
                                                    ClearAndDisplayTripAndUserLocationOnMapEffect(
                                                        map,
                                                        trip,
                                                        userLocation
                                                    ),
                                                    AnimateMapToTripEffect(map, trip, userLocation)
                                                )
                                            }
                                            userLocation != null -> {
                                                setOf(
                                                    AddUserLocationToMap(map, userLocation),
                                                    MoveMapEffect(map, userLocation)
                                                )
                                            }
                                            else -> {
                                                setOf()
                                            }
                                        }
                                    } else {
                                        setOf()
                                    }
                                    newState.withEffects(effects)
                                } else {
                                    state.withEffects()
                                }
                            }
                            is InitMapAction -> {
                                reduce(action, state, userScope)
                            }
                            OnResumeAction,
                            OnPauseAction,
                            OnGeofencesUpdateAction,
                            is TripUpdatedAction,
                            is UserLocationChangedAction -> {
                                //do nothing
                                state.withEffects()
                            }
                            OnAddOrderClickAction,
                            OnCompleteClickAction,
                            OnMyLocationClickAction,
                            is OnOrderClickAction,
                            OnTripFocusedAction -> {
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
        appState: Initialized,
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
