package com.hypertrack.android.ui.common

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.add_integration.AddIntegrationViewModel
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.add_geotag.AddGeotagViewModel
import com.hypertrack.android.ui.screens.outage.OutageViewModel
import com.hypertrack.android.ui.screens.background_permissions.BackgroundPermissionsViewModel
import com.hypertrack.android.ui.screens.select_trip_destination.SelectTripDestinationViewModel
import com.hypertrack.android.ui.screens.send_feedback.SendFeedbackViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.BaseHistoryStyle
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesVisitsViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewModel

@Suppress("UNCHECKED_CAST")
class UserScopeViewModelFactory(
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
                userScope.integrationsRepository
            ) as T
            AddPlaceViewModel::class.java -> AddPlaceViewModel(
                baseDependencies,
                userScope.placesInteractor,
                userScope.googlePlacesInteractor,
                appScope.geocodingInteractor,
                userScope.deviceLocationProvider,
            ) as T
            SelectDestinationViewModel::class.java -> SelectDestinationViewModel(
                baseDependencies,
                userScope.placesInteractor,
                userScope.googlePlacesInteractor,
                appScope.geocodingInteractor,
                userScope.deviceLocationProvider,
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
                userScope.summaryInteractor,
                appScope.distanceFormatter,
                appScope.timeFormatter
            ) as T
            ProfileViewModel::class.java -> ProfileViewModel(
                baseDependencies,
                userScope.measurementUnitsRepository,
                appScope.userRepository,
                appScope.publishableKeyRepository,
                userScope.hyperTrackService,
            ) as T
            SelectTripDestinationViewModel::class.java -> SelectTripDestinationViewModel(
                baseDependencies,
                userScope.placesInteractor,
                userScope.googlePlacesInteractor,
                appScope.geocodingInteractor,
                userScope.deviceLocationProvider,
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
