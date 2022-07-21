package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.interactors.app.optics.GeofencesForMapOptic
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.screens.add_order.AddOrderViewModel
import com.hypertrack.android.ui.screens.add_order_info.AddOrderInfoViewModel
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.place_details.PlaceDetailsViewModel
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModelDependencies
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.add_order_info.AddOrderParams
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.formatters.MetersDistanceFormatter
import com.hypertrack.android.utils.mapState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
class ParamViewModelFactory<T>(
    private val param: T,
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val userScope: UserScope,
    private val useCases: UseCases
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseDependencies = BaseViewModelDependencies(
            appInteractor,
            appScope.osUtilsProvider,
            appScope.osUtilsProvider,
            appScope.crashReportsProvider
        )
        val userLoggedInFlow = appInteractor.appStateFlow.mapState(appScope.appCoroutineScope) {
            AppStateOptics.getUserLoggedIn(it)
        }
        val mapUiReducer = MapUiReducer()
        val selectDestinationViewModelDependencies = SelectDestinationViewModelDependencies(
            userLoggedInFlow,
            userScope.googlePlacesInteractor,
            appScope.geocodingInteractor,
            userScope.deviceLocationProvider,
            userScope.mapUiReducer,
            userScope.mapUiEffectHandler
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
                    appInteractor.appStateFlow.mapState(appScope.appCoroutineScope) {
                        AppStateOptics.getUserLoggedIn(it)
                    },
                    userScope.placesInteractor,
                    appScope.geocodingInteractor,
                    userScope.integrationsRepository,
                    MetersDistanceFormatter(
                        appScope.osUtilsProvider,
                        userScope.measurementUnitsRepository
                    ),
                    appScope.mapItemsFactory,
                    userScope.mapUiReducer,
                    userScope.mapUiEffectHandler,
                    useCases.logExceptionToCrashlyticsUseCase
                ) as T
            }
            AddOrderInfoViewModel::class.java -> AddOrderInfoViewModel(
                param as AddOrderParams,
                baseDependencies,
                userScope.tripsInteractor,
                appScope.geocodingInteractor,
            ) as T
            AddOrderViewModel::class.java -> AddOrderViewModel(
                param as String,
                baseDependencies,
                selectDestinationViewModelDependencies
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
