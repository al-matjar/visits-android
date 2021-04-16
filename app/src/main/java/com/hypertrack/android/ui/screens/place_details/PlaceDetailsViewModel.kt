package com.hypertrack.android.ui.screens.place_details

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.common.KeyValueItem
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.launch

class PlaceDetailsViewModel(
    val geofenceId: String,
    val placesRepository: PlacesRepository,
    val osUtilsProvider: OsUtilsProvider
) : BaseViewModel() {

    private val map = MutableLiveData<GoogleMap>()

    val loadingState = MutableLiveData<Boolean>(false)

    private val geofence = MutableLiveData<Geofence>().apply {
        try {
            postValue(placesRepository.getGeofence(geofenceId))
        } catch (e: Exception) {
            //todo handle
        }
    }

    val address = Transformations.map(geofence) { geofence ->
        geofence.fullAddress ?: osUtilsProvider.getPlaceFromCoordinates(
            geofence.latitude,
            geofence.longitude
        )?.toAddressString()
    }

    val metadata: LiveData<List<KeyValueItem>> = Transformations.map(geofence) { geofence ->
        (geofence.metadata?.filter { it.value is String } ?: mapOf())
            .toMutableMap().apply {
                put("visits_count", geofence.visitsCount.toString())
//                put("created_at", geofence.created_at.toString())
            }
            .map { KeyValueItem(it.key, it.value as String) }.toList()
    }

    val visits: LiveData<List<GeofenceMarker>> = Transformations.map(geofence) { geofence ->
        geofence.marker?.markers?.sortedByDescending { it.arrival!!.recordedAt } ?: listOf()
    }

    val externalMapsIntent = MutableLiveData<Intent>()

    init {
        //todo check leak
        ZipLiveData(geofence, map).apply {
            observeForever {
                displayGeofenceLocation(it.first, it.second)
            }
        }
    }

    fun onMapReady(googleMap: GoogleMap) {
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = false
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = true
        }
        map.postValue(googleMap)
    }

    fun displayGeofenceLocation(geofence: Geofence, googleMap: GoogleMap) {
        googleMap.addMarker(
            MarkerOptions().position(geofence.latLng).title(geofence.name)
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geofence.latLng, 13.0f))
    }

    fun onDirectionsClick() {
//        val gmmIntentUri = Uri.parse("google.navigation:q=${geofence.value!!.latitude},${geofence.value!!.longitude}")

        val googleMapsUrl = "https://www.google.com/maps/dir/?api=1&" +
                "destination=${geofence.value!!.latitude},${geofence.value!!.longitude}"

        val gmmIntentUri = Uri.parse(googleMapsUrl)
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        externalMapsIntent.postValue(mapIntent)
//        mapIntent.setPackage("com.google.android.apps.maps")
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

    fun onRefresh() {
        viewModelScope.launch {
            loadingState.postValue(true)
            placesRepository.refreshGeofences()
            loadingState.postValue(false)
        }
    }

    fun onCopyVisitIdClick(str: String) {
        osUtilsProvider.copyToClipboard(str)
    }
}