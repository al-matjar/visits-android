package com.hypertrack.android.ui.screens.place_details

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.interactors.GeofenceError
import com.hypertrack.android.interactors.GeofenceSuccess
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ZipNotNullableLiveData
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.KeyValueItem
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.util.format

import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch

class PlaceDetailsViewModel(
    private val geofenceId: String,
    private val placesInteractor: PlacesInteractor,
    private val datetimeFormatter: DatetimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeFormatter,
    private val moshi: Moshi,
    baseDependencies: BaseViewModelDependencies
) : BaseViewModel(baseDependencies) {

    private val addressDelegate = GeofenceAddressDelegate(osUtilsProvider)

    private val mapWrapper = MutableLiveData<HypertrackMapWrapper>()

    private val geofence = MutableLiveData<LocalGeofence>().apply {
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

    val address = Transformations.map(geofence) { geofence ->
        addressDelegate.fullAddress(geofence)
    }

    val metadata: LiveData<List<KeyValueItem>> = Transformations.map(geofence) { geofence ->
        geofence.metadata.toMutableMap().apply {
            put(
                "Geofence ID",
                geofence.id
            )
            put(
                osUtilsProvider.stringFromResource(R.string.created_at),
                datetimeFormatter.formatDatetime(geofence.createdAt)
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

    private fun displayGeofenceLocation(geofence: LocalGeofence, mapWrapper: HypertrackMapWrapper) {
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
            MyApplication.injector.getOsUtilsProvider(MyApplication.context),
            datetimeFormatter,
            distanceFormatter,
            timeFormatter
        ) {
            onCopyVisitIdClick(it)
        }
    }
}