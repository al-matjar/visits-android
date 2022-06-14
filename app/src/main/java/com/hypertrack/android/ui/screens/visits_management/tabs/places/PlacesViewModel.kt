package com.hypertrack.android.ui.screens.visits_management.tabs.places

import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class PlacesViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val locationProvider: DeviceLocationProvider,
    private val distanceFormatter: DistanceFormatter,
    private val dateTimeFormatter: DateTimeFormatter,
    private val geofenceAddressDelegate: GeofenceAddressDelegate,
    private val geofenceNameDelegate: GeofenceNameDelegate,
) : BaseViewModel(baseDependencies) {

    private var nextPageToken: String? = null
    private var updateJob: Job? = null

    val placesPage = SingleLiveEvent<Consumable<List<PlaceItem>>?>()

    fun refresh() {
        placesInteractor.invalidateCache()
        init()
    }

    fun init() {
        loadingState.value = false
        loadingState.postValue(false)
        updateJob?.cancel()
        nextPageToken = null
        placesPage.value = null
        placesPage.postValue(null)
        onLoadMore()
    }

    fun createPlacesAdapter(): PlacesAdapter {
        return PlacesAdapter(
            osUtilsProvider,
            locationProvider,
            distanceFormatter,
            dateTimeFormatter,
            resourceProvider
        )
    }

    fun onPlaceClick(placeItem: PlaceItem) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                placeItem.geofenceId
            )
        )
    }

    fun onAddPlaceClicked() {
        destination.postValue(VisitsManagementFragmentDirections.actionVisitManagementFragmentToAddPlaceFragment())
    }

    fun onLoadMore() {
        if ((loadingState.value ?: false) == false) {
            //todo change to viewModelScope (viewModelScope cause bug when launch is not called after geofence creation)
            updateJob = GlobalScope.launch {
                try {
                    if (nextPageToken != null || placesPage.value == null) {
//                        Log.v("hypertrack-verbose", "** loading ${nextPageToken.hashCode()}")
                        loadingState.postValue(true)
                        val res = placesInteractor.loadPage(nextPageToken)
                        nextPageToken = res.paginationToken
//                        Log.v("hypertrack-verbose", "nextPageToken = ${nextPageToken.hashCode()}")
                        placesPage.postValue(Consumable(mapAdapterItems(res.items)))
                        loadingState.postValue(false)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        showExceptionMessageAndReport(e)
                        loadingState.postValue(false)
                    }
                }
            }
        }
    }

    private suspend fun mapAdapterItems(geofences: List<Geofence>): List<PlaceItem> {
        val addresses = mutableMapOf<Geofence, String?>()
        try {
            withTimeout(5000L) {
                //todo parallelize
                geofences.forEach { geofence ->
                    addresses[geofence] = geofenceAddressDelegate.shortAddress(geofence)
                }
            }
        } catch (e: TimeoutCancellationException) {
        }
        return geofences.map {
            PlaceItem(
                it,
                displayAddress = addresses[it]
                    ?: resourceProvider.stringFromResource(R.string.address_not_available),
                geofenceDisplayName = geofenceNameDelegate.getName(it)
            )
        }
    }

}
