package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.map_state.MapUiState
import com.hypertrack.android.ui.common.map_state.OnMapMovedMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateGeofenceForDetailsMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateMapViewMapUiAction
import com.hypertrack.android.ui.common.map_state.UpdateGeofenceForDetailsEffect
import com.hypertrack.android.ui.common.use_case.get_error_message.ComplexTextError
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.ui.common.use_case.get_error_message.UnknownError
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.map
import com.hypertrack.android.utils.withEffects
import com.hypertrack.logistics.android.github.R

class AddPlaceInfoReducer(
    //todo move formatting to effects
    private val distanceFormatter: DistanceFormatter,
    private val mapUiReducer: MapUiReducer
) {

    fun reduce(
        action: Action,
        state: State,
        userState: UserLoggedIn
    ): ReducerResult<out State, out Effect> {
        return when (state) {
            Initial -> {
                when (action) {
                    is MapReadyAction -> {
                        state.withEffects(InitEffect(action.map))
                    }
                    is InitFinishedAction -> {
                        val defaultRadius = PlacesInteractor.DEFAULT_RADIUS

                        mapUiReducer.reduce(
                            UpdateMapViewMapUiAction(action.map),
                            MapUiState(
                                action.map
                            )
                        ).withState {
                            Initialized(
                                it,
                                action.location,
                                if (action.hasIntegrations) {
                                    IntegrationsEnabled(null)
                                } else {
                                    IntegrationsDisabled(action.geofenceName)
                                },
                                action.address,
                                defaultRadius
                            )
                        }.withEffects { result ->
                            val mapEffects = result.effects.map { MapUiEffect(it) }
                            val radiusEffect = setOf(
                                StartUpdateRadiusEffect(
                                    result.newState.location,
                                    result.newState.radius
                                )
                            )
                            mapEffects + radiusEffect
                        }
                    }
                    is OnErrorAction -> {
                        reduce(action)
                    }
                    is MapUiAction,
                    is MapMovedAction,
                    is GeofenceNameChangedAction,
                    is ConfirmClickedAction -> {
                        // ignore
                        state.withEffects()
                    }
                    IntegrationDeletedAction,
                    GeofenceNameClickedAction,
                    is AddressChangedAction,
                    is CreateGeofenceAction,
                    is GeofenceCreationErrorAction,
                    is IntegrationAddedAction,
                    is RadiusChangedAction -> {
                        // illegal action
                        illegalAction(action, state)
                    }
                }
            }
            is Initialized -> {
                when (action) {
                    is MapReadyAction -> {
                        mapUiReducer.reduce(
                            UpdateMapViewMapUiAction(action.map),
                            state.mapUiState
                        ).withState {
                            state.copy(mapUiState = it)
                        }.withEffects { result ->
                            result.effects.map { MapUiEffect(it) }
                        }
                    }
                    is ConfirmClickedAction -> {
                        reduce(action, state, userState.useCases)
                    }
                    is OnErrorAction -> {
                        reduce(action)
                    }
                    GeofenceNameClickedAction -> {
                        when (state.integrations) {
                            is IntegrationsEnabled -> {
                                state.withEffects(OpenAddIntegrationScreenEffect)
                            }
                            is IntegrationsDisabled -> {
                                state.withEffects()
                            }
                        }
                    }
                    is AddressChangedAction -> {
                        state.copy(address = action.address).withEffects()
                    }
                    is IntegrationAddedAction -> {
                        when (state.integrations) {
                            is IntegrationsEnabled -> {
                                state.copy(integrations = IntegrationsEnabled(action.integration))
                                    .withEffects()
                            }
                            is IntegrationsDisabled -> {
                                illegalAction(action, state)
                            }
                        }
                    }
                    IntegrationDeletedAction -> {
                        when (state.integrations) {
                            is IntegrationsEnabled -> {
                                state.copy(integrations = IntegrationsEnabled(null))
                                    .withEffects()
                            }
                            is IntegrationsDisabled -> {
                                illegalAction(action, state)
                            }
                        }
                    }
                    is RadiusChangedAction -> {
                        state.copy(radius = action.radius).withEffects(
                            StartUpdateRadiusEffect(
                                state.location,
                                action.radius
                            )
                        )
                    }
                    is CreateGeofenceAction -> {
                        if (state.radius != null) {
                            CreatingGeofence(state).withEffects(
                                CreateGeofenceEffect(
                                    GeofenceCreationData(
                                        integration = when (state.integrations) {
                                            is IntegrationsDisabled -> null
                                            is IntegrationsEnabled -> state.integrations.integration
                                        },
                                        radius = state.radius,
                                        params = action.params,
                                    )
                                )
                            )
                        } else {
                            ErrorState(R.string.add_place_info_radius_null.asError()).withEffects()
                        }
                    }
                    is InitFinishedAction, is GeofenceCreationErrorAction -> {
                        illegalAction(action, state)
                    }
                    is GeofenceNameChangedAction -> {
                        when (state.integrations) {
                            is IntegrationsDisabled -> {
                                state.copy(integrations = IntegrationsDisabled(action.name))
                                    .withEffects()
                            }
                            is IntegrationsEnabled -> {
                                state.withEffects()
                            }
                        }
                    }
                    is MapMovedAction -> {
                        mapUiReducer.reduce(
                            OnMapMovedMapUiAction(action.position),
                            state.mapUiState
                        ).withState {
                            state.copy(mapUiState = it)
                        }.withEffects { result ->
                            result.effects.map { MapUiEffect(it) }
                        }
                    }
                    is MapUiAction -> {
                        mapUiReducer.reduce(action.action, state.mapUiState)
                            .withState {
                                state.copy(mapUiState = it)
                            }.withEffects { result ->
                                when (action.action) {
                                    is UpdateGeofenceForDetailsMapUiAction -> {
                                        result.effects.map {
                                            when (it) {
                                                is UpdateGeofenceForDetailsEffect -> {
                                                    UpdateRadiusAndZoomEffect(
                                                        state.location,
                                                        state.radius,
                                                        it
                                                    )
                                                }
                                                else -> {
                                                    MapUiEffect(it)
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        result.effects.map { MapUiEffect(it) }
                                    }
                                }
                            }
                    }
                }
            }
            is ErrorState -> {
                state.withEffects()
            }
            is CreatingGeofence -> {
                when (action) {
                    is GeofenceCreationErrorAction -> {
                        state.previousState.withEffects(
                            ShowErrorMessageEffect(action.exception.asError())
                        )
                    }
                    is OnErrorAction -> {
                        reduce(action)
                    }
                    else -> {
                        state.withEffects()
                    }
                }
            }
        }
    }

    private fun reduce(
        action: ConfirmClickedAction,
        state: Initialized,
        useCases: UserScopeUseCases
    ): ReducerResult<State, Effect> {
        val radiusValid = state.radius?.let { radius ->
            radius >= PlacesInteractor.MIN_RADIUS && radius <= PlacesInteractor.MAX_RADIUS
        } ?: false

        val geofenceNameValid = when (state.integrations) {
            is IntegrationsDisabled -> true
            is IntegrationsEnabled -> state.integrations.integration != null
        }

        return when {
            !radiusValid -> {
                state.withEffects(
                    ShowErrorMessageEffect(
                        ComplexTextError(
                            R.string.add_place_geofence_radius_validation_error,
                            arrayOf(
                                distanceFormatter.formatDistance(PlacesInteractor.MIN_RADIUS),
                                distanceFormatter.formatDistance(PlacesInteractor.MAX_RADIUS)
                            )
                        )
                    )
                )
            }
            !geofenceNameValid -> {
                state.withEffects(
                    ShowErrorMessageEffect(R.string.add_place_info_confirm_disabled.asError())
                )
            }
            else -> {
                if (state.radius != null) {
                    state.withEffects(
                        ProceedWithAdjacentGeofenceCheckEffect(
                            action.params,
                            state.radius,
                            useCases
                        )
                    )
                } else {
                    ErrorState(R.string.add_place_info_radius_null.asError()).withEffects()
                }
            }
        }
    }

    private fun illegalAction(action: Action, state: State): ReducerResult<State, Effect> {
        return ErrorState(UnknownError).withEffects(
            LogExceptionToCrashlyticsEffect(IllegalActionException(action, state))
        )
    }

    private fun reduce(action: OnErrorAction): ReducerResult<State, Effect> {
        return ErrorState(action.error).withEffects(
            when (action.error) {
                is ExceptionError -> setOf(LogExceptionToCrashlyticsEffect(action.error.exception))
                is ComplexTextError, is TextError -> setOf()
            }
        )
    }

}
