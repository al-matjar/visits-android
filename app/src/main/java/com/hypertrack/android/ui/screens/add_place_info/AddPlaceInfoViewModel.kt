@file:Suppress("OPT_IN_USAGE")

package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.config.GservicesValue.value
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.address.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.MetersDistanceFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//todo persist address state in create geofence scope?
class AddPlaceInfoViewModel(
    private val latLng: LatLng,
    private val initialAddress: String?,
    private val _name: String?,
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val geocodingInteractor: GeocodingInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val distanceFormatter: MetersDistanceFormatter,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase
) : BaseViewModel(baseDependencies) {

    val viewState = MutableLiveData<ViewState>()
    val adjacentGeofenceDialogEvent = MutableLiveData<Consumable<GeofenceCreationParams>>()

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)
    private lateinit var geofencesMapDelegate: GeofencesMapDelegate

    private val reducer = AddPlaceInfoReducer(distanceFormatter)
    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        Initial,
        viewModelScope,
        Dispatchers.Main,
        reducer::reduce,
        this::applyEffects,
        this::stateChangeEffects
    )
    private val effectsHandler = AddPlaceInfoEffectsHandler(
        latLng,
        this::init,
        this::handleAction,
        this::displayRadius,
        viewState,
        destination,
        adjacentGeofenceDialogEvent,
        placesInteractor,
        getErrorMessageUseCase,
        showErrorUseCase,
        logExceptionToCrashlyticsUseCase,
    )

    private var radiusShapes: List<Circle>? = null

    fun handleAction(action: Action) {
        stateMachine.handleAction(action)
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(context: Context, googleMap: GoogleMap) {
        val mapWrapper = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = false,
                enableZoomKeys = true,
                enableMyLocationButton = false,
                enableMyLocationIndicator = false
            )
        )
        geofencesMapDelegate = object : GeofencesMapDelegate(
            context,
            mapWrapper,
            placesInteractor,
            osUtilsProvider,
            {}
        ) {
            override fun updateGeofencesOnMap(
                mapWrapper: HypertrackMapWrapper,
                geofences: List<Geofence>
            ) {
                super.updateGeofencesOnMap(mapWrapper, geofences)
                handleAction(UpdateMapDataAction)
            }
        }
        googleMap.setOnCameraIdleListener {
            geofencesMapDelegate.onCameraIdle()
        }

        handleAction(MapReadyAction(mapWrapper))
    }

    fun onConfirmClicked(params: GeofenceCreationParams) {
        handleAction(ConfirmClickedAction(params))
    }

    fun onAddIntegration() {
        handleAction(GeofenceNameClickedAction)
    }

    fun onIntegrationAdded(integration: Integration) {
        handleAction(IntegrationAddedAction(integration))
    }

    fun onDeleteIntegrationClicked() {
        handleAction(IntegrationDeletedAction)
    }

    fun onAddressChanged(address: String) {
        handleAction(AddressChangedAction(address))
    }

    fun onGeofenceNameChanged(name: String) {
        handleAction(GeofenceNameChangedAction(name))
    }

    fun onRadiusChanged(text: String) {
        handleAction(RadiusChangedAction(text))
    }

    fun onGeofenceDialogYes(params: GeofenceCreationParams) {
        handleAction(CreateGeofenceAction(params))
    }

    override fun onCleared() {
        super.onCleared()
        if (this::geofencesMapDelegate.isInitialized) {
            geofencesMapDelegate.onCleared()
        }
    }


    private fun applyEffects(effects: Set<Effect>) {
        viewModelScope.launch {
            try {
                effects.forEach { effect ->
                    effectsHandler.applyEffect(effect)
                }
            } catch (e: Exception) {
                handleErrorFlow(e).collect {
                    handleAction(it)
                }
            }
        }
    }

    private fun stateChangeEffects(newState: State): Set<Effect> {
        return setOf(UpdateViewStateEffect(newState)) + when (newState) {
            is Initialized -> {
                setOf(DisplayRadiusEffect(newState.map, newState.radius))
            }
            else -> setOf()
        }
    }

    private fun displayRadius(map: HypertrackMapWrapper, radius: Int?) {
        radiusShapes?.forEach { it.remove() }
        radiusShapes = map.addGeofenceShape(latLng, radius ?: RADIUS_SHAPE_NULL_VALUE)
        try {
            map.googleMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.builder().apply {
                        include(
                            SphericalUtil.computeOffset(
                                latLng,
                                (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                DEGREES_0
                            )
                        )
                        include(
                            SphericalUtil.computeOffset(
                                latLng,
                                (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                DEGREES_90
                            )
                        )
                        include(
                            SphericalUtil.computeOffset(
                                latLng,
                                (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                DEGREES_180
                            )
                        )
                        include(
                            SphericalUtil.computeOffset(
                                latLng,
                                (radius ?: RADIUS_CIRCLE_NULL_VALUE).toDouble(),
                                DEGREES_270
                            )
                        )
                    }.build(),
                    MAP_CAMERA_PADDING
                )
            )
        } catch (e: Exception) {
            map.moveCamera(latLng, HypertrackMapWrapper.DEFAULT_ZOOM)
            // todo log to crashlytics
        }
    }


    private suspend fun init(map: HypertrackMapWrapper) {
        suspend { integrationsRepository.hasIntegrations() }.asFlow().flatMapConcat { res ->
            when (res) {
                is ResultSuccess -> {
                    suspend { loadAddress() }.asFlow().map { address ->
                        InitFinishedAction(
                            map = map,
                            hasIntegrations = res.value,
                            address = address,
                            geofenceName = _name,
                        )
                    }
                }
                is ResultError -> {
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
                geocodingInteractor.getPlaceFromCoordinates(latLng)?.let {
                    addressDelegate.strictAddress(it)
                }
            }
    }

    companion object {
        const val RADIUS_SHAPE_NULL_VALUE = 1
        const val RADIUS_CIRCLE_NULL_VALUE = 50
        const val DEGREES_0 = 0.0
        const val DEGREES_90 = 90.0
        const val DEGREES_180 = 180.0
        const val DEGREES_270 = 270.0
        const val MAP_CAMERA_PADDING = 50
    }
}

class GeofenceCreationParams(
    val name: String,
    val address: String,
    val description: String
)

data class GeofenceCreationData(
    val params: GeofenceCreationParams,
    val radius: Int,
    val integration: Integration?
)
