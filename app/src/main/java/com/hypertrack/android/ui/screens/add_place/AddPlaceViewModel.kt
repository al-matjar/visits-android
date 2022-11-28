package com.hypertrack.android.ui.screens.add_place

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.models.local.GeofenceId
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModelDependencies
import com.hypertrack.android.ui.common.select_destination.reducer.Proceed
import com.hypertrack.android.ui.common.select_destination.toDestinationData
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.mapState
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// todo refactor to use mapUiReducer to display radius
class AddPlaceViewModel(
    baseDependencies: BaseViewModelDependencies,
    dependencies: SelectDestinationViewModelDependencies,
) : SelectDestinationViewModel(
    baseDependencies,
    dependencies
) {
    private var radiusCircle: Circle? = null
    private val geofencesForMapStateFlow = userState.mapState(appInteractor.appScope.actionsScope) {
        it?.geofencesForMap
    }

    val adjacentGeofenceDialog = MutableLiveData<Consumable<DestinationData>>()

    override val loadingState = MediatorLiveData<Boolean>().apply {
        addSource(geofencesForMapStateFlow.map { it?.isLoadingForLocation ?: false }.asLiveData()) {
            postValue(it)
        }
    }

    override val defaultZoom: Float = 16f

    override fun onMapInitialized(map: HypertrackMapWrapper) {
        displayRadius(map)
    }

    override fun handleEffect(proceed: Proceed) {
        val destinationData = proceed.placeData.toDestinationData()
        viewModelScope.launch {
            loadingState.postValue(true)
            proceed.useCases.checkForAdjacentGeofencesUseCase.execute(
                destinationData.latLng,
                PlacesInteractor.DEFAULT_RADIUS,
                geofencesForMapStateFlow,
                useCachedOnly = true
            ).collect { result ->
                loadingState.postValue(false)
                when (result) {
                    is Success -> {
                        val has = result.data
                        if (has) {
                            adjacentGeofenceDialog.postValue(Consumable(destinationData))
                        } else {
                            proceed(destinationData)
                        }
                    }
                    is Failure -> {
                        showExceptionMessageAndReport(result.exception)
                    }
                }
            }
        }
    }

    private fun onGeofenceDialogYes(destinationData: DestinationData) {
        proceed(destinationData)
    }

    private fun onGeofenceDialogNo() {
    }

    fun createConfirmationDialog(context: Context, destinationData: DestinationData): AlertDialog {
        // todo "ignore adjacent geofence" dialog
        return if (/*placesInteractor.adjacentGeofencesAllowed*/false) {
            AlertDialog.Builder(context)
                .setMessage(
                    R.string.add_place_confirm_adjacent.stringFromResource()
                )
                .setPositiveButton(R.string.yes) { dialog, which ->
                    onGeofenceDialogYes(destinationData)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                }
                .setOnDismissListener {
                    onGeofenceDialogNo()
                }
                .create()
        } else {
            AlertDialog.Builder(context)
                .setMessage(
                    R.string.add_place_adjacent_not_allowed.stringFromResource()
                )
                .setNegativeButton(R.string.close) { _, _ ->
                }
                .create()
        }
    }

    override fun onCameraMoved(map: HypertrackMapWrapper) {
        super.onCameraMoved(map)
        displayRadius(map)
    }

    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                destinationData
            )
        )
    }

    private fun displayRadius(map: HypertrackMapWrapper) {
        radiusCircle?.remove()
        radiusCircle = map.addNewGeofenceRadius(
            map.cameraPosition,
            PlacesInteractor.DEFAULT_RADIUS
        )
    }

}
