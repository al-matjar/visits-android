package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.add_order.AddOrderViewModel
import com.hypertrack.android.ui.screens.add_order_info.AddOrderInfoViewModel
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.place_details.PlaceDetailsViewModel
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.add_order_info.AddOrderParams
import com.hypertrack.android.utils.formatters.MetersDistanceFormatter
import kotlinx.coroutines.FlowPreview

@FlowPreview
@Suppress("UNCHECKED_CAST")
class ParamViewModelFactory<T>(
    private val param: T,
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val userScope: UserScope,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseDependencies = BaseViewModelDependencies(
            appScope.osUtilsProvider,
            appScope.osUtilsProvider,
            appScope.crashReportsProvider
        )
        return when (modelClass) {
            PlaceDetailsViewModel::class.java -> PlaceDetailsViewModel(
                geofenceId = param as String,
                userScope.placesInteractor,
                appScope.geofenceAddressDelegate,
                appScope.geofenceVisitDisplayDelegate,
                appScope.dateTimeFormatter,
                appScope.distanceFormatter,
                appScope.timeFormatter,
                baseDependencies
            ) as T
            OrderDetailsViewModel::class.java -> OrderDetailsViewModel(
                orderId = param as String,
                baseDependencies,
                userScope.tripsInteractor,
                userScope.photoUploadQueueInteractor,
                appScope.dateTimeFormatter,
            ) as T
            AddPlaceInfoViewModel::class.java -> (param as DestinationData).let { destinationData ->
                AddPlaceInfoViewModel(
                    destinationData.latLng,
                    initialAddress = destinationData.address,
                    _name = destinationData.name,
                    baseDependencies,
                    userScope.placesInteractor,
                    appScope.geocodingInteractor,
                    userScope.integrationsRepository,
                    MetersDistanceFormatter(
                        appScope.osUtilsProvider,
                        userScope.measurementUnitsRepository
                    ),
                ) as T
            }
            AddOrderInfoViewModel::class.java -> AddOrderInfoViewModel(
                param as AddOrderParams,
                baseDependencies,
                appInteractor,
                userScope.tripsInteractor,
                appScope.geocodingInteractor,
            ) as T
            AddOrderViewModel::class.java -> AddOrderViewModel(
                param as String,
                baseDependencies,
                userScope.placesInteractor,
                userScope.googlePlacesInteractor,
                appScope.geocodingInteractor,
                userScope.deviceLocationProvider,
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
