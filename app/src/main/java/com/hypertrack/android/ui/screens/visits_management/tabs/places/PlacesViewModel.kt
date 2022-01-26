package com.hypertrack.android.ui.screens.visits_management.tabs.places

import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlacesViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val locationProvider: DeviceLocationProvider,
    private val distanceFormatter: DistanceFormatter,
    private val dateTimeFormatter: DateTimeFormatter,
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
            dateTimeFormatter
        )
    }

    fun onPlaceClick(placeItem: PlaceItem) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                placeItem.geofence.id
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
                        placesPage.postValue(Consumable(res.items.map { PlaceItem(it) }))
                        loadingState.postValue(false)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        errorHandler.postException(e)
                        loadingState.postValue(false)
                    }
                }
            }
        }
    }

}
