package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.screens.add_integration.AddIntegrationViewModel
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModelDependencies
import com.hypertrack.android.ui.screens.add_geotag.AddGeotagViewModel
import com.hypertrack.android.ui.screens.outage.OutageViewModel
import com.hypertrack.android.ui.screens.select_trip_destination.SelectTripDestinationViewModel
import com.hypertrack.android.ui.screens.send_feedback.SendFeedbackViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesVisitsViewModel
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.mapState
import java.time.LocalDate

@Suppress("UNCHECKED_CAST")
class UserScopeViewModelFactory(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val userScope: UserScope,
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
        val selectDestinationViewModelDependencies = SelectDestinationViewModelDependencies(
            userLoggedInFlow,
            userScope.googlePlacesInteractor,
            appScope.geocodingInteractor,
            userScope.deviceLocationProvider,
            MapUiReducer(),
            MapUiEffectHandler(appInteractor)
        )
        return when (modelClass) {
            OutageViewModel::class.java -> OutageViewModel(
                baseDependencies
            ) as T
            SendFeedbackViewModel::class.java -> SendFeedbackViewModel(
                baseDependencies,
                userScope.feedbackInteractor,
            ) as T
            OrdersListViewModel::class.java -> OrdersListViewModel(
                baseDependencies,
                userScope.tripsInteractor,
                userScope.tripsUpdateTimerInteractor,
                appScope.dateTimeFormatter,
                appScope.orderAddressDelegate,
            ) as T
            AddIntegrationViewModel::class.java -> AddIntegrationViewModel(
                baseDependencies,
                appInteractor.appStateFlow.mapState(appScope.appCoroutineScope) {
                    AppStateOptics.getUserLoggedIn(it)
                },
                userScope.integrationsRepository
            ) as T
            AddPlaceViewModel::class.java -> AddPlaceViewModel(
                baseDependencies,
                selectDestinationViewModelDependencies
            ) as T
            SelectDestinationViewModel::class.java -> SelectDestinationViewModel(
                baseDependencies,
                selectDestinationViewModelDependencies
            ) as T
            PlacesViewModel::class.java -> PlacesViewModel(
                baseDependencies,
                userScope.placesInteractor,
                userScope.deviceLocationProvider,
                appScope.distanceFormatter,
                appScope.dateTimeFormatter,
                appScope.geofenceAddressDelegate,
                appScope.geofenceNameDelegate
            ) as T
            SummaryViewModel::class.java -> SummaryViewModel(
                baseDependencies,
                appInteractor.appStateFlow.mapState(appScope.appCoroutineScope) {
                    AppStateOptics.getHistoryState(it)?.days?.get(LocalDate.now()) ?: Loading()
                },
                appScope.distanceFormatter,
                appScope.timeFormatter
            ) as T
            ProfileViewModel::class.java -> ProfileViewModel(
                baseDependencies,
                userScope.measurementUnitsRepository,
                appScope.userRepository,
                appScope.publishableKeyRepository,
                userScope.hyperTrackService,
                appScope.accessTokenRepository
            ) as T
            SelectTripDestinationViewModel::class.java -> SelectTripDestinationViewModel(
                baseDependencies,
                selectDestinationViewModelDependencies
            ) as T
            PlacesVisitsViewModel::class.java -> PlacesVisitsViewModel(
                baseDependencies,
                userScope.placesVisitsInteractor,
                appScope.geofenceVisitDisplayDelegate,
                appScope.dateTimeFormatter,
                appScope.distanceFormatter,
            ) as T
            AddGeotagViewModel::class.java -> AddGeotagViewModel(
                baseDependencies,
                userScope.geotagsInteractor
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
