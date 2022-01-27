package com.hypertrack.android.ui.screens.add_place

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.Circle
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.delegates.GeofenceId
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.reducer.Proceed
import com.hypertrack.android.ui.common.select_destination.toDestinationData
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch


class AddPlaceViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val deviceLocationProvider: DeviceLocationProvider,
) : SelectDestinationViewModel(
    baseDependencies,
    placesInteractor,
    googlePlacesInteractor,
    deviceLocationProvider,
) {
    val adjacentGeofenceDialog = MutableLiveData<Consumable<DestinationData>>()

    override val loadingState = placesInteractor.isLoadingForLocation

    override val errorHandler = ErrorHandler(
        osUtilsProvider,
        crashReportsProvider,
        placesInteractor.errorFlow.asLiveData()
    )

    private var radiusCircle: Circle? = null

    override val defaultZoom: Float = 16f

    override fun handleEffect(proceed: Proceed) {
        val destinationData = proceed.placeData.toDestinationData()
        viewModelScope.launch {
            loadingState.postValue(true)
            val has = placesInteractor.hasAdjacentGeofence(
                destinationData.latLng,
                PlacesInteractor.DEFAULT_RADIUS
            )
            loadingState.postValue(false)
            if (has) {
                adjacentGeofenceDialog.postValue(Consumable(destinationData))
                return@launch
            } else {
                proceed(destinationData)
            }
        }
    }

    private fun onGeofenceDialogYes(destinationData: DestinationData) {
        proceed(destinationData)
    }

    private fun onGeofenceDialogNo() {
    }

    fun createConfirmationDialog(context: Context, destinationData: DestinationData): AlertDialog {
        return if (placesInteractor.adjacentGeofencesAllowed) {
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

    override fun createGeofencesMapDelegate(
        context: Context,
        wrapper: HypertrackMapWrapper,
        markerClickListener: (GeofenceId) -> Unit
    ): GeofencesMapDelegate {
        return object : GeofencesMapDelegate(
            context,
            wrapper,
            placesInteractor,
            osUtilsProvider,
            markerClickListener
        ) {
            override fun updateGeofencesOnMap(
                mapWrapper: HypertrackMapWrapper,
                geofences: List<LocalGeofence>
            ) {
                super.updateGeofencesOnMap(mapWrapper, geofences)
                displayRadius(mapWrapper)
            }
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
