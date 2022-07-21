package com.hypertrack.android.ui.screens.visits_management.tabs.places

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.PlaceVisitsStats
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.delegates.display.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success

import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.*

class PlacesVisitsViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesVisitsInteractor: PlacesVisitsInteractor,
    private val displayDelegate: GeofenceVisitDisplayDelegate,
    private val dateTimeFormatter: DateTimeFormatter,
    private val distanceFormatter: DistanceFormatter,
) : BaseViewModel(baseDependencies) {

    val visitsStats = MutableLiveData<List<VisitItem>>()

    fun onPullToRefresh() {
        placesVisitsInteractor.invalidateCache()
        init()
    }

    fun onResume() {
        init()
    }

    fun createVisitsAdapter(): AllPlacesVisitsAdapter {
        return AllPlacesVisitsAdapter(
            dateTimeFormatter,
            distanceFormatter
        ) {
            osUtilsProvider.copyToClipboard(it)
        }.apply {
            onItemClickListener = {
                if (it is Visit) {
                    onVisitClick(it)
                }
            }
        }
    }

    private fun init() {
        loadingState.postValue(true)
        viewModelScope.launch {
            placesVisitsInteractor.getPlaceVisitsStats().let {
                loadingState.postValue(false)
                when (it) {
                    is Success -> {
                        visitsStats.postValue(mapListData(it.data))
                    }
                    is Failure -> {
                        showExceptionMessageAndReport(it.exception)
                    }
                }
            }
        }
    }

    private fun onVisitClick(visitItem: Visit) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                visitItem.geofenceId
            )
        )
    }

    private fun mapListData(visitsStats: PlaceVisitsStats): List<VisitItem> {
        val stats = visitsStats.data
        return mutableListOf<VisitItem>().apply {
            stats.forEach { (k, v) ->
                add(Day(k, v.totalDriveDistance))
                v.visits.forEach {
                    add(mapVisit(it))
                }
            }
        }
    }

    fun onStateChangedToVisits() {
        onResume()
    }

    private fun mapVisit(visit: LocalGeofenceVisit): Visit {
        return Visit(
            visit.getDay(),
            // todo report and filter visits with id == null
            visitId = visit.id ?: "null",
            geofenceId = visit.geofenceId,
            title = displayDelegate.getGeofenceName(visit),
            durationText = displayDelegate.getDurationText(visit),
            dateTimeText = dateTimeFormatter.formatDateTime(visit.arrival.value),
            routeToText = displayDelegate.getRouteToText(visit),
            integrationName = visit.metadata?.integration?.name,
            addressText = visit.address
                ?: resourceProvider.stringFromResource(R.string.address_not_available)
        )
    }

}


