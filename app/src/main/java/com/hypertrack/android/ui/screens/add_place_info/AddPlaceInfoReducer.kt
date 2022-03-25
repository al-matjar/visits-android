package com.hypertrack.android.ui.screens.add_place_info

import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.IllegalActionException
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AddPlaceInfoReducer(
    private val resourceProvider: ResourceProvider,
    private val distanceFormatter: DistanceFormatter
) {

    fun reduce(state: State, action: Action): ReducerResult<State, Effect> {
        return when (state) {
            Initial -> {
                when (action) {
                    is MapReadyAction -> {
                        state.withEffects(InitEffect(action.map))
                    }
                    is InitFinishedAction -> {
                        Initialized(
                            action.map,
                            if (action.hasIntegrations) {
                                IntegrationsEnabled(null)
                            } else {
                                IntegrationsDisabled(action.geofenceName)
                            },
                            action.address,
                            PlacesInteractor.DEFAULT_RADIUS
                        ).withEffects()
                    }
                    UpdateMapDataAction, is GeofenceNameChangedAction -> {
                        state.withEffects()
                    }
                    is ErrorAction -> {
                        Error(action.exception).withEffects()
                    }
                    else -> {
                        if (MyApplication.DEBUG_MODE) {
                            Error(IllegalActionException(action, state)).withEffects()
                        } else {
                            state.withEffects()
                        }
                    }
                }
            }
            is Initialized -> {
                when (action) {
                    is MapReadyAction -> {
                        state.copy(map = action.map).withEffects()
                    }
                    UpdateMapDataAction -> {
                        state.withEffects()
                    }
                    is ConfirmClickedAction -> {
                        reduce(action, state)
                    }
                    is ErrorAction -> {
                        Error(action.exception).withEffects()
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
                                Error(IllegalActionException(action, state)).withEffects()
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
                                Error(IllegalActionException(action, state)).withEffects()
                            }
                        }
                    }
                    is RadiusChangedAction -> {
                        try {
                            val radius = if (action.radiusString.isNotBlank()) {
                                action.radiusString.toInt()
                            } else {
                                null
                            }
                            state.copy(radius = radius).withEffects()
                        } catch (e: Exception) {
                            //todo crashlytics
                            Error(e).withEffects()
                        }
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
                            Error(IllegalStateException("radius == null")).withEffects()
                        }
                    }
                    is InitFinishedAction, is GeofenceCreationErrorAction -> {
                        Error(IllegalActionException(action, state)).withEffects()
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
                }
            }
            is Error -> {
                state.withEffects()
            }
            is CreatingGeofence -> {
                when (action) {
                    is GeofenceCreationErrorAction -> {
                        state.previousState.withEffects(
                            ShowErrorMessageEffect(ErrorMessage(action.exception).text)
                        )
                    }
                    is ErrorAction -> {
                        Error(action.exception).withEffects()
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
        state: Initialized
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
                        resourceProvider.stringFromResource(
                            R.string.add_place_geofence_radius_validation_error,
                            distanceFormatter.formatDistance(PlacesInteractor.MIN_RADIUS),
                            distanceFormatter.formatDistance(PlacesInteractor.MAX_RADIUS)
                        )
                    )
                )
            }
            !geofenceNameValid -> {
                state.withEffects(
                    ShowErrorMessageEffect(
                        resourceProvider.stringFromResource(
                            R.string.add_place_info_confirm_disabled
                        )
                    )
                )
            }
            else -> {
                if (state.radius != null) {
                    state.withEffects(
                        ProceedWithAdjacentGeofenceCheckEffect(
                            action.params,
                            state.radius,
                        )
                    )
                } else {
                    Error(IllegalStateException("radius must not be null"))
                        .withEffects()
                }
            }
        }
    }

}
