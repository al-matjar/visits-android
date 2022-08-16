package com.hypertrack.android.ui.common.select_destination.reducer

import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.AddGeofencesMapUiAction
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.ui.common.map_state.OnMapMovedMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateMapViewMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateUserLocationMapUiAction
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.withEffects

class SelectDestinationViewModelReducer(
    private val mapUiReducer: MapUiReducer
) {

    fun reduceAction(
        state: State,
        action: Action,
        userState: UserLoggedIn
    ): ReducerResult<out State, Effect> {
        return when (state) {
            is MapReady -> {
                when (action) {
                    is UserLocationReceived -> {
                        // todo change to subscription
                        // todo move map to user location only one time at start
                        val userLocation = UserLocation(action.latLng, action.address)
                        mapUiReducer.reduce(
                            UpdateUserLocationMapUiAction(action.latLng),
                            state.mapUiState
                        ).withState {
                            state.copy(
                                userLocation = userLocation,
                                mapUiState = it
                            )
                        }.withEffects { result ->
                            val mapUiEffects = result.effects.map { MapUiEffect(it) }.toSet()
                            mapUiEffects + getMapMoveEffectsIfNeeded(
                                state.waitingForUserLocationMove,
                                userLocation,
                                state.map
                            )
                        }
                    }
                    is MapReadyAction -> {
                        mapUiReducer.reduce(
                            UpdateMapViewMapUiAction(action.map),
                            state.mapUiState
                        ).withState {
                            state.copy(mapUiState = it)
                        }.withEffects { result ->
                            result.effects.map { MapUiEffect(it) }.toSet()
                        }
                    }
                    is MapCameraMoved -> {
                        @Suppress("RedundantIf")
                        when (state.flow) {
                            is AutocompleteFlow -> {
                                state.withEffects()
                            }
                            MapFlow -> {
                                when (action.cause) {
                                    MovedToPlace -> {
                                        state.withEffects()
                                    }
                                    MovedToUserLocation, MovedByUser -> {
                                        mapUiReducer.reduce(
                                            OnMapMovedMapUiAction(action.latLng),
                                            state.mapUiState
                                        ).withState {
                                            MapReady(
                                                it,
                                                state.userLocation,
                                                LocationSelected(
                                                    action.latLng,
                                                    action.address
                                                ),
                                                MapFlow,
                                                //if user performed map move or clicked a place (which leads to programmatic move)
                                                //we don't need to move map to his location anymore
                                                //unless it was first map movement near zero coordinates on map init
                                                waitingForUserLocationMove = if (!action.isNearZero) {
                                                    false
                                                } else {
                                                    true
                                                }
                                            )
                                        }.withEffects {
                                            val mapEffects =
                                                it.effects.map { MapUiEffect(it) }.toSet()
                                            mapEffects + setOf(
                                                DisplayLocationInfo(action.address, null)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is PlaceSelectedAction -> {
                        val place = PlaceSelected(
                            latLng = action.latLng,
                            displayAddress = action.displayAddress,
                            strictAddress = action.strictAddress,
                            name = action.name,
                        )

                        state.withPlaceSelected(place, MapFlow)
                            .withEffects(
                                CloseKeyboard,
                                ClearSearchQuery,
                                RemoveSearchFocus,
                                MoveMapToPlace(place, state.map),
                                DisplayLocationInfo(
                                    address = place.displayAddress,
                                    placeName = place.name
                                ),
                            )
                    }
                    is MapClicked -> {
                        when (state.flow) {
                            MapFlow -> state.withEffects()
                            is AutocompleteFlow -> state.withMapFlow(MapFlow)
                                .withEffects(
                                    CloseKeyboard,
                                    ClearSearchQuery,
                                    RemoveSearchFocus,
                                    DisplayLocationInfo(action.address, null)
                                )
                        }
                    }
                    is SearchQueryChanged -> {
                        state.withAutocompleteFlow(AutocompleteFlow(action.results))
                            .withEffects()
                    }
                    is AutocompleteError -> {
                        state.withMapFlow(MapFlow)
                            .withEffects(CloseKeyboard)
                    }
                    ConfirmClicked -> {
                        state.withEffects(
                            CloseKeyboard,
                            Proceed(state.placeData, userState.useCases)
                        )
                    }
                    ShowMyLocationAction -> {
                        state.withEffects(
                            if (state.userLocation != null) {
                                setOf(
                                    AnimateMapToUserLocation(
                                        state.userLocation,
                                        state.map
                                    )
                                )
                            } else setOf()
                        )
                    }
                    is GeofencesOnMapUpdatedAction -> {
                        mapUiReducer.reduce(
                            AddGeofencesMapUiAction(action.geofences),
                            state.mapUiState
                        ).withState {
                            state.copy(mapUiState = it)
                        }.withEffects { result ->
                            result.effects.map { MapUiEffect(it) }.toSet()
                        }
                    }
                }
            }
            is MapNotReady -> {
                when (action) {
                    is MapReadyAction -> {
                        val userLocationEffects = getMapMoveEffectsIfNeeded(
                            state.waitingForUserLocationMove,
                            state.userLocation,
                            action.map
                        )

                        mapUiReducer.reduce(
                            UpdateMapViewMapUiAction(action.map), MapUiState(
                                action.map,
                                userLocation = state.userLocation?.latLng
                            )
                        ).withState {
                            MapReady(
                                it,
                                state.userLocation,
                                LocationSelected(
                                    action.cameraPosition,
                                    action.address
                                ),
                                MapFlow,
                                waitingForUserLocationMove = if (state.waitingForUserLocationMove) {
                                    userLocationEffects.isEmpty()
                                } else {
                                    false
                                }
                            )
                        }.withEffects { result ->
                            val mapUiEffects = result.effects.map { MapUiEffect(it) }
                            userLocationEffects + mapUiEffects + setOf(
                                HideProgressbar
                            )
                        }
                    }
                    is UserLocationReceived -> {
                        val userLocation = UserLocation(action.latLng, action.address)
                        state.withUserLocation(userLocation).withEffects()
                    }
                    ShowMyLocationAction,
                    ConfirmClicked,
                    is MapCameraMoved,
                    is GeofencesOnMapUpdatedAction -> {
                        // ignore
                        state.withEffects()
                    }
                    is AutocompleteError,
                    is SearchQueryChanged,
                    is MapClicked,
                    is PlaceSelectedAction -> {
                        illegalAction(action, state)
                    }
                }
            }
        }
    }

    private fun getMapMoveEffectsIfNeeded(
        waitingForUserLocationMove: Boolean,
        userLocation: UserLocation?,
        map: HypertrackMapWrapper
    ): Set<Effect> {
        return if (waitingForUserLocationMove && userLocation != null) {
            setOf(
                MoveMapToUserLocation(userLocation, map),
                DisplayLocationInfo(userLocation.address, null)
            )
        } else {
            setOf()
        }
    }

    private fun illegalAction(action: Action, state: State): ReducerResult<State, Effect> {
        return if (MyApplication.DEBUG_MODE) {
            throw IllegalActionException(action, state)
        } else {
            state.withEffects()
        }
    }

    companion object {
        val INITIAL_STATE = MapNotReady(null, true)
    }
}
