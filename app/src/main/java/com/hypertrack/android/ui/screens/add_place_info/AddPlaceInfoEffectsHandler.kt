package com.hypertrack.android.ui.screens.add_place_info

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.use_case.ShowErrorUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class AddPlaceInfoEffectsHandler(
    private val placeLocation: LatLng,
    private val init: suspend (map: HypertrackMapWrapper) -> Unit,
    private val handleAction: (action: Action) -> Unit,
    private val displayRadius: suspend (map: HypertrackMapWrapper, radius: Int?) -> Unit,
    private val viewState: MutableLiveData<ViewState>,
    private val destination: MutableLiveData<Consumable<NavDirections>>,
    private val adjacentGeofenceDialogEvent: MutableLiveData<Consumable<GeofenceCreationParams>>,
    private val placesInteractor: PlacesInteractor,
    private val getErrorMessageUseCase: GetErrorMessageUseCase,
    private val showErrorUseCase: ShowErrorUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
) {

    suspend fun applyEffect(effect: Effect) {
        when (effect) {
            is UpdateViewStateEffect -> {
                getViewStateFlow(effect.state).collect {
                    viewState.postValue(it)
                }
            }
            is DisplayRadiusEffect -> {
                displayRadius(effect.map, effect.radius)
            }
            OpenAddIntegrationScreenEffect -> {
                destination.postValue(
                    AddPlaceInfoFragmentDirections.actionAddPlaceInfoFragmentToAddIntegrationFragment()
                )
            }
            is ShowErrorMessageEffect -> {
                handleEffect(effect)
            }
            is CreateGeofenceEffect -> {
                handleEffect(effect)
            }
            is ProceedWithAdjacentGeofenceCheckEffect -> {
                handleEffect(effect)
            }
            is InitEffect -> {
                init(effect.map)
            }
            is LogExceptionToCrashlytics -> {
                logExceptionToCrashlyticsUseCase.execute(effect.exception).collect()
            }
        } as Any?
    }

    private suspend fun handleEffect(effect: ProceedWithAdjacentGeofenceCheckEffect) {
        val radius = effect.radius
        val params = effect.params
        if (placesInteractor.adjacentGeofencesAllowed) {
            //check adjacent geofences without waiting for them to fully load (only in cache)
            placesInteractor.hasAdjacentGeofence(placeLocation, radius).let { has ->
                if (!has) {
                    handleAction(CreateGeofenceAction(params))
                } else {
                    adjacentGeofenceDialogEvent.postValue(Consumable(effect.params))
                }
            }
        } else {
            placesInteractor.blockingHasAdjacentGeofence(placeLocation, radius)
                .let { has ->
                    if (!has) {
                        handleAction(CreateGeofenceAction(params))
                    } else {
                        handleEffect(
                            ShowErrorMessageEffect(
                                R.string.add_place_info_adjacent_geofence_error.asError()
                            )
                        )
                    }
                }
        }
    }

    private suspend fun handleEffect(effect: CreateGeofenceEffect) {
        val radius = effect.geofenceCreationData.radius
        val params = effect.geofenceCreationData.params
        val integration = effect.geofenceCreationData.integration
        val res = placesInteractor.createGeofence(
            latitude = placeLocation.latitude,
            longitude = placeLocation.longitude,
            radius = radius,
            name = params.name,
            address = params.address,
            description = params.description,
            integration = integration
        )
        when (res) {
            is CreateGeofenceSuccess -> {
                destination.postValue(
                    AddPlaceFragmentDirections.actionGlobalVisitManagementFragment(
                        Tab.PLACES
                    )
                )
            }
            is CreateGeofenceError -> {
                handleAction(GeofenceCreationErrorAction(res.e))
            }
        }
    }

    private suspend fun handleEffect(effect: ShowErrorMessageEffect) {
        showErrorUseCase.execute(effect.error).collect()
    }

    private fun getViewStateFlow(newState: State): Flow<ViewState> {
        return when (newState) {
            is Initial -> {
                ViewState(
                    isLoading = true,
                    address = null,
                    radius = null,
                    enableConfirmButton = false,
                    errorMessage = null,
                    integrationsViewState = createIntegrationsViewState(
                        IntegrationsDisabled(null)
                    )
                ).toFlow()
            }
            is Initialized -> {
                ViewState(
                    isLoading = false,
                    address = newState.address,
                    radius = newState.radius,
                    enableConfirmButton = when (newState.integrations) {
                        is IntegrationsDisabled -> true
                        is IntegrationsEnabled -> newState.integrations.integration != null
                    },
                    errorMessage = null,
                    integrationsViewState = createIntegrationsViewState(newState.integrations),
                ).toFlow()
            }
            is CreatingGeofence -> {
                ViewState(
                    isLoading = true,
                    address = null,
                    radius = null,
                    enableConfirmButton = false,
                    errorMessage = null,
                    integrationsViewState = createIntegrationsViewState(
                        IntegrationsDisabled(null)
                    )
                ).toFlow()
            }
            is ErrorState -> {
                getErrorMessageUseCase.execute(newState.error).map { errorMessage ->
                    ViewState(
                        isLoading = false,
                        address = null,
                        radius = null,
                        enableConfirmButton = false,
                        errorMessage = errorMessage,
                        integrationsViewState = createIntegrationsViewState(
                            IntegrationsDisabled(null)
                        )
                    )
                }
            }
        }
    }

    private fun createIntegrationsViewState(
        integrationsState: IntegrationsState
    ): IntegrationsViewState {
        return when (integrationsState) {
            is IntegrationsDisabled -> NoIntegrations(
                R.string.add_place_geofence_name,
                integrationsState.geofenceName
            )
            is IntegrationsEnabled -> HasIntegrations(
                if (integrationsState.integration != null) {
                    ShowIntegration(integrationsState.integration)
                } else {
                    ShowGeofenceName(R.string.add_place_company_name)
                }
            )
        }
    }


}
