package com.hypertrack.android.ui.common.select_destination

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.GeofencesForMapUpdatedEvent
import com.hypertrack.android.models.local.GeofenceId
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.delegates.address.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.map.viewportPosition
import com.hypertrack.android.ui.common.select_destination.reducer.Action
import com.hypertrack.android.ui.common.select_destination.reducer.AnimateMapToUserLocation
import com.hypertrack.android.ui.common.select_destination.reducer.AutocompleteError
import com.hypertrack.android.ui.common.select_destination.reducer.AutocompleteFlow
import com.hypertrack.android.ui.common.select_destination.reducer.ClearSearchQuery
import com.hypertrack.android.ui.common.select_destination.reducer.CloseKeyboard
import com.hypertrack.android.ui.common.select_destination.reducer.ConfirmClicked
import com.hypertrack.android.ui.common.select_destination.reducer.DisplayLocationInfo
import com.hypertrack.android.ui.common.select_destination.reducer.Effect
import com.hypertrack.android.ui.common.select_destination.reducer.GeofencesOnMapUpdatedAction
import com.hypertrack.android.ui.common.select_destination.reducer.HideProgressbar
import com.hypertrack.android.ui.common.select_destination.reducer.LocationSelected
import com.hypertrack.android.ui.common.select_destination.reducer.MapCameraMoved
import com.hypertrack.android.ui.common.select_destination.reducer.MapClicked
import com.hypertrack.android.ui.common.select_destination.reducer.MapFlow
import com.hypertrack.android.ui.common.select_destination.reducer.MapNotReady
import com.hypertrack.android.ui.common.select_destination.reducer.MapReady
import com.hypertrack.android.ui.common.select_destination.reducer.MapReadyAction
import com.hypertrack.android.ui.common.select_destination.reducer.MapUiEffect
import com.hypertrack.android.ui.common.select_destination.reducer.MoveMapToPlace
import com.hypertrack.android.ui.common.select_destination.reducer.MoveMapToUserLocation
import com.hypertrack.android.ui.common.select_destination.reducer.MovedByUser
import com.hypertrack.android.ui.common.select_destination.reducer.MovedToPlace
import com.hypertrack.android.ui.common.select_destination.reducer.MovedToUserLocation
import com.hypertrack.android.ui.common.select_destination.reducer.PlaceData
import com.hypertrack.android.ui.common.select_destination.reducer.PlaceSelected
import com.hypertrack.android.ui.common.select_destination.reducer.PlaceSelectedAction
import com.hypertrack.android.ui.common.select_destination.reducer.Proceed
import com.hypertrack.android.ui.common.select_destination.reducer.RemoveSearchFocus
import com.hypertrack.android.ui.common.select_destination.reducer.SearchQueryChanged
import com.hypertrack.android.ui.common.select_destination.reducer.SelectDestinationViewModelReducer
import com.hypertrack.android.ui.common.select_destination.reducer.ShowMyLocationAction
import com.hypertrack.android.ui.common.select_destination.reducer.State
import com.hypertrack.android.ui.common.select_destination.reducer.UserLocationReceived
import com.hypertrack.android.ui.common.util.isNearZero
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asNonEmpty
import com.hypertrack.android.utils.exception.IllegalActionException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

open class SelectDestinationViewModel(
    baseDependencies: BaseViewModelDependencies,
    dependencies: SelectDestinationViewModelDependencies
) : BaseViewModel(baseDependencies) {
    protected val userState = dependencies.userState
    private val googlePlacesInteractor = dependencies.googlePlacesInteractor
    private val geocodingInteractor = dependencies.geocodingInteractor
    private val deviceLocationProvider = dependencies.deviceLocationProvider
    private val mapUiReducer = dependencies.mapUiReducer
    private val mapUiEffectHandler = dependencies.mapUiEffectHandler

    //    private val enableLogging = MyApplication.DEBUG_MODE
    private val enableLogging = true

    protected open val defaultZoom = 13f

    private val reducer = SelectDestinationViewModelReducer(mapUiReducer)
    protected lateinit var state: State

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)
    private val placesDelegate = GooglePlacesSearchDelegate(
        crashReportsProvider,
        googlePlacesInteractor
    )

    private var isMapMovingToPlace: Boolean = false
    private var isMapMovingToUserLocation: Boolean = false

    val searchQuery = MutableLiveData<String>()
    val locationInfo = MutableLiveData<DisplayLocationInfo>()
    val showConfirmButton = MutableLiveData<Boolean>()
    val showMyLocationButton = MutableLiveData<Boolean>()
    val placesResults = MutableLiveData<List<GooglePlaceModel>>()
    val closeKeyboard = SingleLiveEvent<Boolean>()
    val goBackEvent = SingleLiveEvent<DestinationData>()
    val removeSearchFocusEvent = SingleLiveEvent<Boolean>()

    init {
        runInVmEffectsScope {
            appInteractor.appEvent.collect {
                when (it) {
                    is GeofencesForMapUpdatedEvent -> {
                        handleAction(GeofencesOnMapUpdatedAction(it.geofences))
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun handleAction(action: Action) {
        viewModelScope.launch {
            userState.value?.let { userLoggedIn ->
                val actionLog = "action = $action"
                if (enableLogging) Log.v("hypertrack-verbose", actionLog)
                crashReportsProvider.log(actionLog)
                try {
                    val res = reducer.reduceAction(state, action, userLoggedIn)
                    applyState(res.newState)
                    applyEffects(res.effects)
                } catch (e: Exception) {
                    if (MyApplication.DEBUG_MODE) {
                        throw e
                    } else {
                        showExceptionMessageAndReport(e)
                    }
                }
            } ?: showExceptionMessageAndReport(IllegalActionException(action, userState.value))
        }
    }

    private fun applyState(state: State) {
        when (state) {
            is MapNotReady -> {
                loadingState.postValue(true)
                showConfirmButton.postValue(false)
            }
            is MapReady -> {
                showConfirmButton.postValue(true)

                when (state.flow) {
                    is AutocompleteFlow -> {
                        placesResults.postValue(state.flow.places.elements)
                        showMyLocationButton.postValue(false)
                    }
                    MapFlow -> {
                        placesResults.postValue(listOf())
                        showMyLocationButton.postValue(true)
                    }
                }
            }
        } as Unit
        this.state = state
        val stateLog = "new state = $state"
        if (enableLogging) Log.v("hypertrack-verbose", stateLog)
        crashReportsProvider.log(stateLog)
    }

    private suspend fun applyEffects(effects: Set<Effect>) {
        for (effect in effects) {
            val effectLog = "effect = $effect"
            if (enableLogging) Log.v("hypertrack-verbose", effectLog)
            crashReportsProvider.log(effectLog)
            when (effect) {
                is DisplayLocationInfo -> {
                    locationInfo.postValue(effect)
                }
                CloseKeyboard -> {
                    closeKeyboard.postValue(true)
                }
                is MoveMapToPlace -> {
                    isMapMovingToPlace = true
                    moveMapCamera(effect.map, effect.placeSelected.latLng)
                }
                is MoveMapToUserLocation -> {
                    isMapMovingToUserLocation = true
                    moveMapCamera(effect.map, effect.userLocation.latLng)
                }
                is AnimateMapToUserLocation -> {
                    effect.map.animateCamera(effect.userLocation.latLng, defaultZoom)
                }
                is Proceed -> {
                    handleEffect(effect)
                }
                RemoveSearchFocus -> {
                    removeSearchFocusEvent.postValue(true)
                }
                HideProgressbar -> {
                    loadingState.postValue(false)
                }
                ClearSearchQuery -> {
                    searchQuery.postValue("")
                }
                is MapUiEffect -> {
                    mapUiEffectHandler.getEffectFlow(effect.effect).collect()
                }
            } as Any?
        }
    }

    fun onViewCreated() {
        if (enableLogging) Log.w("hypertrack-verbose", "onViewCreated")
        if (!this::state.isInitialized) {
            state = SelectDestinationViewModelReducer.INITIAL_STATE
            if (enableLogging) Log.v("hypertrack-verbose", "INITIAL_STATE $state")
        } else {
            state = when (val state = state) {
                is MapNotReady -> state
                is MapReady -> MapNotReady(
                    state.userLocation,
                    state.waitingForUserLocationMove
                )
            }
            if (enableLogging) Log.v("hypertrack-verbose", "new state = $state")
        }

        // todo change to subscription
        deviceLocationProvider.getCurrentLocation {
            it?.let { userLocation ->
                viewModelScope.launch {
                    handleAction(
                        UserLocationReceived(
                            userLocation,
                            addressDelegate.displayAddress(
                                geocodingInteractor.getPlaceFromCoordinates(it)
                            )
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    open fun onMapReady(context: Context, googleMap: GoogleMap) {
        val wrapper = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = true,
                enableMyLocationButton = false,
                enableMyLocationIndicator = false,
            )
        )
        wrapper.setOnCameraMovedListener { position ->
            onCameraMoved(wrapper)
            val isMapMovingToPlace = isMapMovingToPlace
            viewModelScope.launch {
                handleAction(
                    MapCameraMoved(
                        position,
                        position.let {
                            addressDelegate.displayAddress(
                                geocodingInteractor.getPlaceFromCoordinates(it)
                            )
                        },
                        cause = when {
                            isMapMovingToPlace -> MovedToPlace
                            isMapMovingToUserLocation -> MovedToUserLocation
                            else -> MovedByUser
                        },
                        isNearZero = position.isNearZero()
                    )
                )
            }
            this.isMapMovingToPlace = false
            isMapMovingToUserLocation = false
        }

        wrapper.setOnMapClickListener {
            viewModelScope.launch {
                handleAction(
                    MapClicked(
                        googleMap.viewportPosition,
                        geocodingInteractor.getPlaceFromCoordinates(googleMap.viewportPosition)
                            .let {
                                addressDelegate.displayAddress(it)
                            })
                )
            }
        }
        wrapper.setOnMarkerClickListener {
            // todo effect
            it.snippet?.let {
                destination.postValue(
                    AddPlaceFragmentDirections.actionGlobalPlaceDetailsFragment(
                        geofenceId = it
                    )
                )
            }
        }

        onMapInitialized(wrapper)

        viewModelScope.launch {
            handleAction(MapReadyAction(
                wrapper,
                googleMap.viewportPosition,
                googleMap.viewportPosition.let {
                    addressDelegate.displayAddress(
                        geocodingInteractor.getPlaceFromCoordinates(
                            it
                        )
                    )
                }
            ))
        }
    }

    protected open fun onMapInitialized(map: HypertrackMapWrapper) {
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            val state = state
            if (state is MapReady) {
                placesDelegate.search(query, state.userLocation?.latLng).let { result ->
                    when (result) {
                        is Success -> {
                            if (result.data.isNotEmpty()) {
                                handleAction(SearchQueryChanged(query, result.data.asNonEmpty()))
                            }
                        }
                        is Failure -> {
                            showExceptionMessageAndReport(result.exception)
                            handleAction(AutocompleteError(query))
                        }
                    }
                }
            }
        }
    }

    fun onConfirmClicked() {
        handleAction(ConfirmClicked)
    }

    fun onPlaceItemClick(item: GooglePlaceModel) {
        viewModelScope.launch {
            placesDelegate.fetchPlace(item).let { result ->
                when (result) {
                    is Success -> {
                        val place = result.data
                        place.latLng?.let { ll ->
                            handleAction(
                                PlaceSelectedAction(
                                    displayAddress = addressDelegate.displayAddress(place),
                                    strictAddress = addressDelegate.strictAddress(place),
                                    name = place.name,
                                    ll
                                )
                            )
                        }
                    }
                    is Failure -> {
                        showExceptionMessageAndReport(result.exception)
                    }
                }
            }
        }
    }

    protected open fun proceed(destinationData: DestinationData) {
        goBackEvent.postValue(destinationData)
    }

    protected open fun handleEffect(proceed: Proceed) {
        proceed(proceed.placeData.toDestinationData())
    }

    protected open fun onCameraMoved(map: HypertrackMapWrapper) {
    }

    private fun moveMapCamera(map: HypertrackMapWrapper, latLng: LatLng) {
        map.moveCamera(latLng, defaultZoom)
    }

    fun onMyLocationClick() {
        handleAction(ShowMyLocationAction)
    }

}

fun PlaceData.toDestinationData(): DestinationData {
    val placeData = this
    return when (placeData) {
        is PlaceSelected -> {
            DestinationData(
                placeData.latLng,
                address = placeData.strictAddress,
                name = placeData.name
            )
        }
        is LocationSelected -> {
            DestinationData(
                placeData.latLng,
                address = placeData.address,
                name = null
            )
        }
    }
}

