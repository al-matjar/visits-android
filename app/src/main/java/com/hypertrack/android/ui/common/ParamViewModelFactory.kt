package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.add_order.AddOrderViewModel
import com.hypertrack.android.ui.screens.add_order_info.AddOrderInfoViewModel
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.place_details.PlaceDetailsViewModel
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.add_order_info.AddOrderParams
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.formatters.MetersDistanceFormatter
import com.squareup.moshi.Moshi
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class ParamViewModelFactory<T>(
    private val param: T,
    private val appScope: AppScope,
    private val userScopeProvider: Provider<UserScope>,
    private val osUtilsProvider: OsUtilsProvider,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseDependencies = BaseViewModelDependencies(
            osUtilsProvider,
            osUtilsProvider,
            crashReportsProvider
        )
        return when (modelClass) {
            PlaceDetailsViewModel::class.java -> PlaceDetailsViewModel(
                geofenceId = param as String,
                userScopeProvider.get().placesInteractor,
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
                userScopeProvider.get().tripsInteractor,
                userScopeProvider.get().photoUploadQueueInteractor,
                appScope.dateTimeFormatter,
                appScope.orderAddressDelegate,
            ) as T
            AddPlaceInfoViewModel::class.java -> (param as DestinationData).let { destinationData ->
                AddPlaceInfoViewModel(
                    destinationData.latLng,
                    initialAddress = destinationData.address,
                    _name = destinationData.name,
                    baseDependencies,
                    userScopeProvider.get().placesInteractor,
                    appScope.geocodingInteractor,
                    userScopeProvider.get().integrationsRepository,
                    MetersDistanceFormatter(
                        osUtilsProvider,
                        userScopeProvider.get().measurementUnitsRepository
                    ),
                ) as T
            }
            AddOrderInfoViewModel::class.java -> AddOrderInfoViewModel(
                param as AddOrderParams,
                baseDependencies,
                userScopeProvider.get().tripsInteractor,
                appScope.geocodingInteractor,
            ) as T
            AddOrderViewModel::class.java -> AddOrderViewModel(
                param as String,
                baseDependencies,
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                appScope.geocodingInteractor,
                userScopeProvider.get().deviceLocationProvider,
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
