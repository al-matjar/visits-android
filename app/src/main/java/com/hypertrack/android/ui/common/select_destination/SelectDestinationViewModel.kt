package com.hypertrack.android.ui.common.select_destination

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.delegates.GeofenceClusterItem
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.select_destination.reducer.*
import com.hypertrack.android.ui.common.util.isNearZero
import com.hypertrack.android.ui.common.util.nullIfEmpty
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.asNonEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

open class SelectDestinationViewModel(
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val crashReportsProvider: CrashReportsProvider
) : BaseViewModel(osUtilsProvider) {

//    private val enableLogging = MyApplication.DEBUG_MODE
private val enableLogging = false

    protected open val defaultZoom = 13f

    private val reducer = SelectDestinationViewModelReducer()
    protected lateinit var state: State

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)
    private val placesDelegate = GooglePlacesSearchDelegate(googlePlacesInteractor)
    protected lateinit var geofencesMapDelegate: GeofencesMapDelegate

    private var isMapMovingToPlace: Boolean = false
    private var isMapMovingToUserLocation: Boolean = false

    val searchQuery = MutableLiveData<String>()
    val locationInfo = MutableLiveData<DisplayLocationInfo>()
    val showConfirmButton = MutableLiveData<Boolean>()
    val placesResults = MutableLiveData<List<GooglePlaceModel>>()
    val closeKeyboard = SingleLiveEvent<Boolean>()
    val goBackEvent = SingleLiveEvent<DestinationData>()
    val removeSearchFocusEvent = SingleLiveEvent<Boolean>()

    protected fun sendAction(action: Action) {
        viewModelScope.launch {
            val actionLog = "action = $action"
            if (enableLogging) Log.v("hypertrack-verbose", actionLog)
            crashReportsProvider.log(actionLog)
            try {
                val res = reducer.reduceAction(state, action)
                applyState(res.newState)
                applyEffects(res.effects)
            } catch (e: Exception) {
                if (MyApplication.DEBUG_MODE) {
                    throw e
                } else {
                    errorHandler.postException(e)
                    crashReportsProvider.logException(e)
                }
            }
        }
    }

    private fun applyState(state: State) {
        when (state) {
            is MapNotReady -> {
                loadingStateBase.postValue(true)
                showConfirmButton.postValue(false)
            }
            is MapReady -> {
                showConfirmButton.postValue(true)

                when (state.flow) {
                    is AutocompleteFlow -> placesResults.postValue(state.flow.places.elements)
                    MapFlow -> placesResults.postValue(listOf())
                }
            }
        }.let { }
        this.state = state
        val stateLog = "new state = $state"
        if (enableLogging) Log.v("hypertrack-verbose", stateLog)
        crashReportsProvider.log(stateLog)
    }

    private fun applyEffects(effects: Set<Effect>) {
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
                is Proceed -> {
                    handleEffect(effect)
                }
                RemoveSearchFocus -> {
                    removeSearchFocusEvent.postValue(true)
                }
                HideProgressbar -> {
                    loadingStateBase.postValue(false)
                }
                ClearSearchQuery -> {
                    searchQuery.postValue("")
                }
            }.let {}
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

        deviceLocationProvider.getCurrentLocation {
            it?.let {
                sendAction(UserLocationReceived(it.toLatLng(), it.toLatLng().let {
                    addressDelegate.displayAddress(osUtilsProvider.getPlaceFromCoordinates(it))
                }))
            }
        }
    }

    @SuppressLint("MissingPermission")
    open fun onMapReady(context: Context, googleMap: GoogleMap) {
        val wrapper = HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableScroll = true,
                enableZoomKeys = true,
                enableMyLocationButton = true,
                enableMyLocationIndicator = true,
            )
        )
        wrapper.setOnCameraMovedListener { position ->
            onCameraMoved(wrapper)
            sendAction(
                MapCameraMoved(
                    position,
                    position.let {
                        addressDelegate.displayAddress(
                            osUtilsProvider.getPlaceFromCoordinates(
                                it
                            )
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
            isMapMovingToPlace = false
            isMapMovingToUserLocation = false
        }

        wrapper.setOnMapClickListener {
            sendAction(
                MapClicked(
                    googleMap.viewportPosition,
                    osUtilsProvider.getPlaceFromCoordinates(googleMap.viewportPosition).let {
                        addressDelegate.displayAddress(it)
                    })
            )
        }

        geofencesMapDelegate = createGeofencesMapDelegate(context, wrapper) {
            it.snippet.nullIfEmpty()?.let { snippet ->
                destination.postValue(
                    AddPlaceFragmentDirections.actionGlobalPlaceDetailsFragment(
                        snippet
                    )
                )
            }
        }
        sendAction(MapReadyAction(
            wrapper,
            googleMap.viewportPosition,
            googleMap.viewportPosition.let {
                addressDelegate.displayAddress(osUtilsProvider.getPlaceFromCoordinates(it))
            }
        ))
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            val state = state
            if (state is MapReady) {
                try {
                    val res = viewModelScope.async {
                        placesDelegate.search(query, state.userLocation?.latLng)
                    }.await()
                    if (res.isNotEmpty()) {
                        sendAction(SearchQueryChanged(query, res.asNonEmpty()))
                    }
                } catch (e: Exception) {
                    errorHandler.postException(e)
                    sendAction(AutocompleteError(query))
                }
            }
        }

    }

    fun onConfirmClicked() {
        sendAction(ConfirmClicked)
    }

    fun onPlaceItemClick(item: GooglePlaceModel) {
        viewModelScope.launch {
            placesDelegate.fetchPlace(item).let { place ->
                place.latLng?.let { ll ->
                    sendAction(
                        PlaceSelectedAction(
                            displayAddress = addressDelegate.displayAddress(place),
                            strictAddress = addressDelegate.strictAddress(place),
                            name = place.name,
                            ll
                        )
                    )
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
        placesInteractor.loadGeofencesForMap(map.cameraPosition)
        geofencesMapDelegate.onCameraIdle()
    }

    private fun moveMapCamera(map: HypertrackMapWrapper, latLng: LatLng) {
        map.moveCamera(latLng, defaultZoom)
    }

    protected open fun createGeofencesMapDelegate(
        context: Context,
        wrapper: HypertrackMapWrapper,
        markerClickListener: (GeofenceClusterItem) -> Unit
    ): GeofencesMapDelegate {
        return GeofencesMapDelegate(
            context,
            wrapper,
            placesInteractor,
            osUtilsProvider,
            markerClickListener
        )
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