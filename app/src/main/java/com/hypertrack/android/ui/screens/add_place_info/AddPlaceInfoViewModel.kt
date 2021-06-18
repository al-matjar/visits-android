package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch


class AddPlaceInfoViewModel(
    private val latLng: LatLng,
    private val initialAddress: String?,
    private val _name: String?,
    private val placesInteractor: PlacesInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val osUtilsProvider: OsUtilsProvider,
) : BaseViewModel() {

    private var hasIntegrations = MutableLiveData<Boolean>(false)

    val loadingState = MutableLiveData<Boolean>(true)

    //todo to baseVM
    val error = SingleLiveEvent<String>()

    //todo persist state in create geofence scope
    val address = MutableLiveData<String?>().apply {
        if (initialAddress != null) {
            postValue(initialAddress)
        } else {
            osUtilsProvider.getPlaceFromCoordinates(latLng.latitude, latLng.longitude)?.let {
                postValue(it.toAddressString())
            }
        }
    }
    val name = MutableLiveData<String>().apply {
        _name?.let {
            postValue(_name)
        }
    }
    val integration = MutableLiveData<Integration?>(null)
    val enableConfirmButton = MediatorLiveData<Boolean>().apply {
        addSource(hasIntegrations) {
            postValue(shouldEnableConfirmButton())
        }
        addSource(integration) {
            postValue(shouldEnableConfirmButton())
        }
    }

    val showGeofenceNameField = MediatorLiveData<Boolean>().apply {
        addSource(hasIntegrations) {
            postValue(shouldShowGeofenceName())
        }
        addSource(integration) {
            postValue(shouldShowGeofenceName())
        }
    }

    init {
        viewModelScope.launch {
            loadingState.postValue(true)
            val res = integrationsRepository.hasIntegrations()
            if (res != null) {
                hasIntegrations.postValue(res)
                loadingState.postValue(false)
            } else {
                error.postValue(osUtilsProvider.stringFromResource(R.string.place_integration_error))
                hasIntegrations.postValue(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        googleMap.addMarker(MarkerOptions().position(latLng))
    }

    fun onConfirmClicked(name: String, address: String, description: String) {
        if (enableConfirmButton.value!!) {
            viewModelScope.launch {
                loadingState.postValue(true)

                val res = placesInteractor.createGeofence(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    name = name,
                    address = address,
                    description = description,
                    integration = integration.value
                )
                loadingState.postValue(false)
                when (res) {
                    is CreateGeofenceSuccess -> {
                        destination.postValue(
                            AddPlaceFragmentDirections.actionGlobalVisitManagementFragment(
                                Tab.PLACES
                            )
                        )
                    }
                    is CreateGeofenceError -> {
                        error.postValue(res.e.message)
                    }
                }
            }
        } else {
            error.postValue(osUtilsProvider.getString(R.string.place_info_confirm_disabled))
        }
    }

    //todo test
    fun onAddIntegration(): Boolean {
        return if (hasIntegrations.value == true) {
            destination.postValue(
                AddPlaceInfoFragmentDirections.actionAddPlaceInfoFragmentToAddIntegrationFragment()
            )
            true
        } else {
            false
        }
    }

    fun onIntegrationAdded(integration: Integration) {
        this.integration.postValue(integration)
    }

    fun onDeleteIntegrationClicked() {
        integration.postValue(null)
    }

    fun onAddressChanged(address: String) {
        if (this.address.value != address) {
            this.address.postValue(address)
        }
    }

    private fun shouldEnableConfirmButton(): Boolean {
        return if (hasIntegrations.value == true) {
            integration.value != null
        } else {
            true
        }
    }

    private fun shouldShowGeofenceName(): Boolean {
        return if (hasIntegrations.value == true) {
            integration.value == null
        } else {
            true
        }
    }

    companion object {
        const val KEY_ADDRESS = "address"
    }
}