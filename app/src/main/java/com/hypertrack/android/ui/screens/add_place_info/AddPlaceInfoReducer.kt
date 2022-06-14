package com.hypertrack.android.ui.screens.add_place_info

import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.common.use_case.get_error_message.ComplexTextError
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.ui.common.use_case.get_error_message.UnknownError
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R

class AddPlaceInfoReducer(
    //todo move formatting to effects
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
                    is OnErrorAction -> {
                        reduce(action)
                    }
                    is AddressChangedAction,
                    is ConfirmClickedAction,
                    is CreateGeofenceAction,
                    is GeofenceCreationErrorAction,
                    GeofenceNameClickedAction,
                    is IntegrationAddedAction,
                    IntegrationDeletedAction,
                    is RadiusChangedAction -> {
                        if (MyApplication.DEBUG_MODE) {
                            ErrorState(UnknownError)
                        } else {
                            state
                        }.withEffects(
                            LogExceptionToCrashlytics(IllegalActionException(action, state))
                        )
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
                        try {
                            val radius = if (action.radiusString.isNotBlank()) {
                                action.radiusString.toInt()
                            } else {
                                null
                            }
                            state.copy(radius = radius).withEffects()
                        } catch (e: Exception) {
                            ErrorState(e.asError()).withEffects(
                                LogExceptionToCrashlytics(e)
                            )
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
            LogExceptionToCrashlytics(IllegalActionException(action, state))
        )
    }

    private fun reduce(errorAction: OnErrorAction): ReducerResult<State, Effect> {
        return ErrorState(errorAction.error).withEffects()
    }

}
