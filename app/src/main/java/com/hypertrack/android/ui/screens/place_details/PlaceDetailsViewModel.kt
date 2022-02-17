package com.hypertrack.android.ui.screens.place_details

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.interactors.GeofenceError
import com.hypertrack.android.interactors.GeofenceSuccess
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ZipNotNullableLiveData
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.common.delegates.address.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.delegates.display.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.util.format

import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class PlaceDetailsViewModel(
    private val geofenceId: String,
    private val placesInteractor: PlacesInteractor,
    private val addressDelegate: GeofenceAddressDelegate,
    private val visitDisplayDelegate: GeofenceVisitDisplayDelegate,
    private val dateTimeFormatter: DateTimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeValueFormatter,
    baseDependencies: BaseViewModelDependencies
) : BaseViewModel(baseDependencies) {


    private val mapWrapper = MutableLiveData<HypertrackMapWrapper>()

    private val geofence = MutableLiveData<Geofence>().apply {
        viewModelScope.launch {
            loadingState.postValue(true)
            when (val res = placesInteractor.getGeofence(geofenceId)) {
                is GeofenceSuccess -> {
                    postValue(res.geofence)
                }
                is GeofenceError -> {
                    errorHandler.postException(res.e)
                }
            }
            loadingState.postValue(false)
        }
    }

    val address = Transformations.switchMap(geofence) { geofence ->
        (suspend { addressDelegate.fullAddress(geofence) }).asFlow().asLiveData()
    }

    val metadata: LiveData<List<KeyValueItem>> = Transformations.map(geofence) { geofence ->
        geofence.metadata.toMutableMap().apply {
            put(
                "Geofence ID",
                geofence.id
            )
            if (geofence.name != null) {
                put(
                    osUtilsProvider.stringFromResource(R.string.place_details_name),
                    geofence.name
                )
            }
            put(
                osUtilsProvider.stringFromResource(R.string.created_at),
                dateTimeFormatter.formatDateTime(geofence.createdAt)
            )
            put(
                osUtilsProvider.stringFromResource(R.string.coordinates),
                geofence.latLng.format()
            )
            put(
                osUtilsProvider.stringFromResource(R.string.place_visits_count),
                geofence.visitsCount.toString()
            )
        }
            .map { KeyValueItem(it.key, it.value) }.toList()
    }

    val integration: LiveData<Integration?> = Transformations.map(geofence) {
        it.integration
    }

    val visits: LiveData<List<LocalGeofenceVisit>> = Transformations.map(geofence) { geofence ->
        geofence.visits
    }

    val externalMapsIntent = MutableLiveData<Consumable<Intent>>()

    init {
        //todo check leak
        ZipNotNullableLiveData(geofence, mapWrapper).apply {
            observeForever {
                displayGeofenceLocation(it.first, it.second)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        mapWrapper.postValue(
            HypertrackMapWrapper(
                googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                    enableScroll = false,
                    enableMyLocationButton = false,
                    enableMyLocationIndicator = true,
                    enableZoomKeys = true
                )
            )
        )
    }

    private fun displayGeofenceLocation(geofence: Geofence, mapWrapper: HypertrackMapWrapper) {
        mapWrapper.addGeofenceShape(geofence)
        mapWrapper.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geofence.latLng, 14.0f))
    }

    fun onDirectionsClick() {
        val intent = osUtilsProvider.getMapsIntent(geofence.value!!.latLng)
        intent?.let {
            externalMapsIntent.postValue(Consumable(it))
        }
    }

    fun onAddressClick() {
        if (!address.value.isNullOrEmpty()) {
            osUtilsProvider.copyToClipboard(address.value!!)
        }
    }

    fun onCopyValue(value: String) {
        if (value.isNotEmpty()) {
            osUtilsProvider.copyToClipboard(value)
        }
    }

    fun onCopyVisitIdClick(str: String) {
        osUtilsProvider.copyToClipboard(str)
    }

    fun onCopyIntegrationName() {
        integration.value?.name?.let {
            osUtilsProvider.copyToClipboard(it)
        }
    }

    fun onCopyIntegrationId() {
        integration.value?.let {
            osUtilsProvider.copyToClipboard(it.id)
        }
    }

    fun createVisitsAdapter(): PlaceVisitsAdapter {
        return PlaceVisitsAdapter(
            visitDisplayDelegate,
        ) {
            onCopyVisitIdClick(it)
        }
    }
}
