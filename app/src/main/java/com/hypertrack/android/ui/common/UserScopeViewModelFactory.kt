package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.interactors.FeedbackInteractor
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.add_integration.AddIntegrationViewModel
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.add_geotag.AddGeotagViewModel
import com.hypertrack.android.ui.screens.select_trip_destination.SelectTripDestinationViewModel
import com.hypertrack.android.ui.screens.send_feedback.SendFeedbackViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.BaseHistoryStyle
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesVisitsViewModel
import com.hypertrack.android.utils.*
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewModel
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class UserScopeViewModelFactory(
    private val appScope: AppScope,
    private val userScopeProvider: Provider<UserScope>,
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val feedbackInteractor: FeedbackInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val driverRepository: DriverRepository,
    private val accountRepository: AccountRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val hyperTrackService: HyperTrackService,
    private val permissionsInteractor: PermissionsInteractor,
    private val accessTokenRepository: AccessTokenRepository,
    private val apiClient: ApiClient,
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseDependencies = BaseViewModelDependencies(
            osUtilsProvider,
            osUtilsProvider,
            crashReportsProvider
        )
        return when (modelClass) {
            SendFeedbackViewModel::class.java -> SendFeedbackViewModel(
                baseDependencies,
                feedbackInteractor,
            ) as T
            OrdersListViewModel::class.java -> OrdersListViewModel(
                baseDependencies,
                tripsInteractor,
                userScopeProvider.get().tripsUpdateTimerInteractor,
                appScope.dateTimeFormatter,
            ) as T
            AddIntegrationViewModel::class.java -> AddIntegrationViewModel(
                baseDependencies,
                integrationsRepository
            ) as T
            AddPlaceViewModel::class.java -> AddPlaceViewModel(
                baseDependencies,
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                deviceLocationProvider,
            ) as T
            CurrentTripViewModel::class.java -> CurrentTripViewModel(
                baseDependencies,
                tripsInteractor,
                placesInteractor,
                userScopeProvider.get().tripsUpdateTimerInteractor,
                hyperTrackService,
                deviceLocationProvider,
                appScope.dateTimeFormatter,
                appScope.timeFormatter
            ) as T
            SelectDestinationViewModel::class.java -> SelectDestinationViewModel(
                baseDependencies,
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                deviceLocationProvider,
            ) as T
            PlacesViewModel::class.java -> PlacesViewModel(
                baseDependencies,
                userScopeProvider.get().placesInteractor,
                deviceLocationProvider,
                appScope.distanceFormatter,
                appScope.dateTimeFormatter
            ) as T
            PermissionRequestViewModel::class.java -> PermissionRequestViewModel(
                baseDependencies,
                permissionsInteractor,
                hyperTrackService
            ) as T
            SummaryViewModel::class.java -> SummaryViewModel(
                baseDependencies,
                userScopeProvider.get().summaryInteractor,
                appScope.distanceFormatter,
                appScope.timeFormatter
            ) as T
            HistoryViewModel::class.java -> HistoryViewModel(
                baseDependencies,
                userScopeProvider.get().historyInteractor,
                appScope.geofenceVisitDisplayDelegate,
                appScope.deviceStatusMarkerDisplayDelegate,
                appScope.geotagDisplayDelegate,
                appScope.timeFormatter,
                appScope.distanceFormatter,
                deviceLocationProvider,
                appScope.mapItemsFactory,
                //todo inject context from AppScope (these changes are in the other PR)
                BaseHistoryStyle(MyApplication.context)
            ) as T
            VisitsManagementViewModel::class.java -> VisitsManagementViewModel(
                baseDependencies,
                userScopeProvider.get().historyInteractorLegacy,
                accountRepository,
                hyperTrackService,
                accessTokenRepository
            ) as T
            ProfileViewModel::class.java -> ProfileViewModel(
                baseDependencies,
                driverRepository,
                hyperTrackService,
                accountRepository,
            ) as T
            SelectTripDestinationViewModel::class.java -> SelectTripDestinationViewModel(
                baseDependencies,
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                deviceLocationProvider,
            ) as T
            PlacesVisitsViewModel::class.java -> PlacesVisitsViewModel(
                baseDependencies,
                userScopeProvider.get().placesVisitsInteractor,
                appScope.geofenceVisitDisplayDelegate,
                appScope.dateTimeFormatter,
                appScope.distanceFormatter,
            ) as T
            AddGeotagViewModel::class.java -> AddGeotagViewModel(
                baseDependencies,
                userScopeProvider.get().geotagsInteractor
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
