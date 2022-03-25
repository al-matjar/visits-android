package com.hypertrack.android.ui.screens.add_place_info

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.logistics.android.github.R

class AddPlaceInfoEffectsHandler(
    private val placeLocation: LatLng,
    private val init: suspend (map: HypertrackMapWrapper) -> Unit,
    private val handleAction: (action: Action) -> Unit,
    private val displayRadius: suspend (map: HypertrackMapWrapper, radius: Int?) -> Unit,
    private val viewState: MutableLiveData<ViewState>,
    private val destination: MutableLiveData<Consumable<NavDirections>>,
    private val errorHandler: ErrorHandler,
    private val adjacentGeofenceDialogEvent: MutableLiveData<Consumable<GeofenceCreationParams>>,
    private val placesInteractor: PlacesInteractor,
    private val resourceProvider: ResourceProvider,
) {

    suspend fun applyEffect(effect: Effect) {
        when (effect) {
            is UpdateViewStateEffect -> {
                viewState.postValue(effect.viewState)
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
                                resourceProvider.stringFromResource(R.string.add_place_info_adjacent_geofence_error)
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

    private fun handleEffect(effect: ShowErrorMessageEffect) {
        errorHandler.postText(effect.text)
    }

}
