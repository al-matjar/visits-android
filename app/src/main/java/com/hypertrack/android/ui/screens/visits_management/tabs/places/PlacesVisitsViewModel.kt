package com.hypertrack.android.ui.screens.visits_management.tabs.places

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.PlaceVisitsStats
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Meters
import com.hypertrack.android.utils.Success

import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter
import com.hypertrack.android.utils.formatters.prettyFormat
import kotlinx.coroutines.*
import java.time.LocalDate

class PlacesVisitsViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesVisitsInteractor: PlacesVisitsInteractor,
    private val datetimeFormatter: DatetimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeFormatter,
) : BaseViewModel(baseDependencies) {

    val visitsStats = MutableLiveData<List<VisitItem>>()

    fun refresh() {
        placesVisitsInteractor.invalidateCache()
        init()
    }

    fun init() {
        loadingState.postValue(true)
        viewModelScope.launch {
            placesVisitsInteractor.getPlaceVisitsStats().let {
                loadingState.postValue(false)
                when (it) {
                    is Success -> visitsStats.postValue(mapListData(it.result))
                    is Failure -> errorHandler.postException(it.exception)
                }
            }
        }
    }

    private fun onVisitClick(visit: LocalGeofenceVisit) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                visit.geofenceId
            )
        )
    }

    fun createVisitsAdapter(): AllPlacesVisitsAdapter {
        return AllPlacesVisitsAdapter(
            osUtilsProvider,
            datetimeFormatter,
            distanceFormatter,
            timeFormatter
        ) {
            osUtilsProvider.copyToClipboard(it)
        }.apply {
            onItemClickListener = {
                if (it is Visit) {
                    onVisitClick(it.visit)
                }
            }
        }
    }

    private fun mapListData(visitsStats: PlaceVisitsStats): List<VisitItem> {
        val stats = visitsStats.data
        return mutableListOf<VisitItem>().apply {
            stats.forEach { (k, v) ->
                add(Day(k, v.totalDriveDistance))
                v.visits.forEach {
                    add(Visit(it))
                }
            }
        }
    }

}

sealed class VisitItem
class Visit(val visit: LocalGeofenceVisit) : VisitItem() {
    override fun toString(): String {
        return "visit ${visit.getDay().prettyFormat()}"
    }
}

class Day(val date: LocalDate, val totalDriveDistance: Meters) : VisitItem() {
    override fun toString(): String {
        return date.prettyFormat()
    }
}

//class MonthItem(val month: Month) : VisitItem() {
//    override fun toString(): String {
//        return month.toString()
//    }
//}