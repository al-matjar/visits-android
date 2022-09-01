@file:Suppress("OPT_IN_USAGE")

package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.app.GeofencesForMapUpdatedEvent
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.delegates.address.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.map_state.AddGeofencesMapUiAction
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.ui.common.util.isNearZero
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.DEGREES_0
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.DEGREES_180
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.DEGREES_270
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.DEGREES_90
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.MAP_CAMERA_PADDING
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.RADIUS_CIRCLE_NULL_VALUE
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoEffectsHandler.Companion.RADIUS_SHAPE_NULL_VALUE
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.formatters.MetersDistanceFormatter
import com.hypertrack.android.utils.state_machine.ReducerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

//todo persist address state in create geofence scope?
class AddPlaceInfoViewModel(
    private val location: LatLng,
    private val initialAddress: String?,
    private val _name: String?,
    baseDependencies: BaseViewModelDependencies,
    private val userStateFlow: StateFlow<UserLoggedIn?>,
    private val placesInteractor: PlacesInteractor,
    private val geocodingInteractor: GeocodingInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val distanceFormatter: MetersDistanceFormatter,
    private val mapItemsFactory: HypertrackMapItemsFactory,
    private val mapUiReducer: MapUiReducer,
    private val mapUiEffectHandler: MapUiEffectHandler,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
) : BaseViewModel(baseDependencies) {

    val viewState = MutableLiveData<ViewState>()
    val adjacentGeofenceDialogEvent = MutableLiveData<Consumable<GeofenceCreationParams>>()

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)

    private val reducer = AddPlaceInfoReducer(distanceFormatter, mapUiReducer)
    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        Initial,
        viewModelScope,
        Dispatchers.Main,
        this::reduce,
        this::applyEffects,
        this::stateChangeEffects
    )
    private val effectsHandler = AddPlaceInfoEffectsHandler(
        location,
        this::init,
        this::handleAction,
        viewState,
        destination,
        adjacentGeofenceDialogEvent,
        placesInteractor,
        mapItemsFactory,
        mapUiEffectHandler,
        getErrorMessageUseCase,
        showErrorUseCase,
        logExceptionToCrashlyticsUseCase,
        userStateFlow.mapState(appInteractor.appScope.appCoroutineScope) { it?.geofencesForMap }
    )

    init {
        runInVmEffectsScope {
            appInteractor.appEvent.collect {
                when (it) {
                    is GeofencesForMapUpdatedEvent -> {
                        handleAction(MapUiAction(AddGeofencesMapUiAction(it.geofences)))
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

    private fun reduce(state: State, action: Action): ReducerResult<out State, out Effect> {
        return userStateFlow.value?.let {
            reducer.reduce(action, state, it)
        } ?: IllegalActionException(action, state).let { exception ->
            state.withEffects(
                LogExceptionToCrashlyticsEffect(exception),
                ShowErrorMessageEffect(exception.asError())
            )
        }
    }


    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = false,
                enableZoomKeys = true,
                enableMyLocationButton = false,
                enableMyLocationIndicator = false
            )
        ).apply {
            setOnCameraMovedListener {
                handleAction(MapMovedAction(it))
            }

            setOnMarkerClickListener {
                //todo effect
                it.snippet?.let {
                    destination.postValue(
                        AddPlaceInfoFragmentDirections.actionGlobalPlaceDetailsFragment(
                            geofenceId = it
                        )
                    )
                }
            }
        }.also {
            handleAction(MapReadyAction(it))
        }
    }

    private fun applyEffects(effects: Set<Effect>) {
        effects.forEach { effect ->
            runInVmEffectsScope {
                effectsHandler.getEffectFlow(effect)
                    .catchException { onError(it) }
                    .collect {
                        it?.let { handleAction(it) }
                    }
            }
        }
    }

    private fun stateChangeEffects(newState: State): Set<Effect> {
        return setOf(UpdateViewStateEffect(newState))
    }

    private suspend fun init(map: HypertrackMapWrapper) {
        suspend { integrationsRepository.hasIntegrations() }.asFlow().flatMapConcat { res ->
            when (res) {
                is Success -> {
                    suspend { loadAddress() }.asFlow().map { address ->
                        InitFinishedAction(
                            map = map,
                            hasIntegrations = res.data,
                            address = address,
                            geofenceName = _name,
                            location = location
                        )
                    }
                }
                is Failure -> {
                    handleErrorFlow(res.exception)
                }
            }
        }.collect {
            handleAction(it)
        }
    }

    private fun handleErrorFlow(exception: Exception): Flow<OnErrorAction> {
        return logExceptionToCrashlyticsUseCase.execute(exception).map {
            OnErrorAction(exception.asError())
        }
    }

    private suspend fun loadAddress(): String? {
        return initialAddress
            ?: withContext(Dispatchers.IO) {
                geocodingInteractor.getPlaceFromCoordinates(location)?.let {
                    addressDelegate.strictAddress(it)
                }
            }
    }

}
