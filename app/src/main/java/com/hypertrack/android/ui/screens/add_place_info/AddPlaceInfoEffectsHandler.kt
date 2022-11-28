package com.hypertrack.android.ui.screens.add_place_info

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.noAction
import com.hypertrack.android.interactors.app.state.GeofencesForMapState
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.UpdateGeofenceForDetailsMapUiAction
import com.hypertrack.android.ui.common.use_case.ShowErrorUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class AddPlaceInfoEffectsHandler(
    private val appInteractor: AppInteractor,
    private val placeLocation: LatLng,
    private val init: suspend (map: HypertrackMapWrapper) -> Unit,
    private val handleAction: (action: Action) -> Unit,
    private val viewState: MutableLiveData<ViewState>,
    private val destination: MutableLiveData<Consumable<NavDirections>>,
    private val adjacentGeofenceDialogEvent: MutableLiveData<Consumable<GeofenceCreationParams>>,
    private val placesInteractor: PlacesInteractor,
    private val mapItemsFactory: HypertrackMapItemsFactory,
    private val mapUiEffectHandler: MapUiEffectHandler,
    private val getErrorMessageUseCase: GetErrorMessageUseCase,
    private val showErrorUseCase: ShowErrorUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
    private val geofencesForMapStateFlow: StateFlow<GeofencesForMapState?>
) {

    fun getEffectFlow(effect: Effect): Flow<Action?> {
        return when (effect) {
            is UpdateViewStateEffect -> {
                getViewStateFlow(effect.state).map {
                    viewState.postValue(it)
                }.noAction()
            }
            OpenAddIntegrationScreenEffect -> {
                destination.updateConsumableAsFlow(
                    AddPlaceInfoFragmentDirections
                        .actionAddPlaceInfoFragmentToAddIntegrationFragment()
                ).noAction()
            }
            is ShowErrorMessageEffect -> {
                suspend { handleEffect(effect) }.asFlow().noAction()
            }
            is CreateGeofenceEffect -> {
                suspend { handleEffect(effect) }.asFlow().noAction()
            }
            is ProceedWithAdjacentGeofenceCheckEffect -> {
                getFlow(effect)
            }
            is InitEffect -> {
                suspend { init(effect.map) }.asFlow().noAction()
            }
            is LogExceptionToCrashlyticsEffect -> {
                logExceptionToCrashlyticsUseCase.execute(effect.exception).noAction()
            }
            is MapUiEffect -> {
                mapUiEffectHandler.getEffectFlow(effect.effect).map { action ->
                    action?.let { MapUiAction(it) }
                }
            }
            is StartUpdateRadiusEffect -> {
                MapUiAction(UpdateGeofenceForDetailsMapUiAction(
                    effect.radius?.let {
                        mapItemsFactory.createGeofenceForDetailView(
                            effect.location,
                            effect.radius
                        )
                    }
                )).toFlow()
            }
            is UpdateRadiusAndZoomEffect -> {
                getFlow(effect)
            }
            is AppActionEffect -> {
                appInteractor.handleActionFlow(effect.appAction).noAction()
            }
        }
    }

    private fun getFlow(effect: UpdateRadiusAndZoomEffect): Flow<Action?> {
        return getEffectFlow(MapUiEffect(effect.updateRadiusEffect))
            .flatMapConcat { action ->
                suspend {
                    val map = effect.updateRadiusEffect.map
                    val location = effect.location
                    val radius = effect.radius
                    try {
                        map.googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.builder().apply {
                                    include(
                                        SphericalUtil.computeOffset(
                                            location,
                                            (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                            DEGREES_0
                                        )
                                    )
                                    include(
                                        SphericalUtil.computeOffset(
                                            location,
                                            (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                            DEGREES_90
                                        )
                                    )
                                    include(
                                        SphericalUtil.computeOffset(
                                            location,
                                            (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                            DEGREES_180
                                        )
                                    )
                                    include(
                                        SphericalUtil.computeOffset(
                                            location,
                                            (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                            DEGREES_270
                                        )
                                    )
                                }.build(),
                                MAP_CAMERA_PADDING
                            )
                        )
                        action
                    } catch (e: Exception) {
                        map.moveCamera(location, HypertrackMapWrapper.DEFAULT_ZOOM)
                        OnErrorAction(e.asError())
                    }
                }.asFlow().flowOn(Dispatchers.Main)
            }
    }

    private fun getFlow(effect: ProceedWithAdjacentGeofenceCheckEffect): Flow<Action?> {
        val radius = effect.radius
        val params = effect.params
        return effect.useCases.checkForAdjacentGeofencesUseCase.execute(
            placeLocation,
            radius,
            geofencesForMapStateFlow
        ).map { result ->
            when (result) {
                is Success -> {
                    val has = result.data
                    if (!has) {
                        CreateGeofenceAction(params)
                    } else {
                        handleEffect(
                            ShowErrorMessageEffect(
                                R.string.add_place_info_adjacent_geofence_error.asError()
                            )
                        )
                        null
                    }
                }
                is Failure -> {
                    OnErrorAction(result.exception.asError())
                }
            }
            // todo "ignore adjacent geofence" dialog
            // adjacentGeofenceDialogEvent.postValue(Consumable(effect.params))
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
                    ),
                    showRetryButton = false
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
                    showRetryButton = false
                ).toFlow()
            }
            is CheckingForAdjacentGeofence -> {
                ViewState(
                    isLoading = true,
                    address = null,
                    radius = null,
                    enableConfirmButton = false,
                    errorMessage = null,
                    integrationsViewState = createIntegrationsViewState(
                        IntegrationsDisabled(null)
                    ),
                    showRetryButton = false
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
                    ),
                    showRetryButton = false
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
                        ),
                        showRetryButton = newState.previousState.let {
                            it is Initialized || it is CreatingGeofence || it is CheckingForAdjacentGeofence
                        }
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

    companion object {
        const val RADIUS_CIRCLE_NULL_VALUE = 50
        const val DEGREES_0 = 0.0
        const val DEGREES_90 = 90.0
        const val DEGREES_180 = 180.0
        const val DEGREES_270 = 270.0
        const val MAP_CAMERA_PADDING = 50
    }


}
