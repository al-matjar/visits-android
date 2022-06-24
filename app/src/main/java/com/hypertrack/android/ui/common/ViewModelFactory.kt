package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.background_permissions.BackgroundPermissionsViewModel
import com.hypertrack.android.ui.screens.confirm_email.ConfirmEmailViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.BaseHistoryStyle
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewModel
import com.hypertrack.android.use_case.app.UseCases

// todo set separate factories for all vms
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val useCases: UseCases
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseViewModelDependencies = BaseViewModelDependencies(
            appScope.osUtilsProvider,
            appScope.osUtilsProvider,
            appScope.crashReportsProvider
        )
        return when (modelClass) {
            ConfirmEmailViewModel::class.java -> ConfirmEmailViewModel(
                baseViewModelDependencies,
                appInteractor,
                useCases.verifyByOtpCodeUseCase,
                useCases.resendExceptionToCrashlyticsUseCase,
                useCases.loadUserStateAfterSignInUseCase,
            ) as T
            SignInViewModel::class.java -> SignInViewModel(
                baseViewModelDependencies,
                appInteractor,
                useCases.signInUseCase,
                useCases.getBranchDataFromAppBackendUseCase,
                useCases.loginWithDeeplinkParamsUseCase,
                useCases.loadUserStateAfterSignInUseCase,
                useCases.logExceptionToCrashlyticsUseCase,
                useCases.logMessageToCrashlyticsUseCase,
                appScope.branchWrapper,
                appScope.moshi
            ) as T
            VisitsManagementViewModel::class.java -> VisitsManagementViewModel(
                baseViewModelDependencies,
                appInteractor.appState,
                appScope.preferencesRepository,
            ) as T
            PermissionRequestViewModel::class.java -> PermissionRequestViewModel(
                baseViewModelDependencies,
                appInteractor,
            ) as T
            HistoryViewModel::class.java -> HistoryViewModel(
                baseViewModelDependencies,
                appInteractor,
                appScope.geocodingInteractor,
                appScope.geofenceVisitAddressDelegate,
                appScope.geofenceVisitDisplayDelegate,
                appScope.deviceStatusMarkerDisplayDelegate,
                appScope.geotagDisplayDelegate,
                appScope.timeFormatter,
                appScope.distanceFormatter,
                appScope.mapItemsFactory,
                BaseHistoryStyle(appScope.appContext)
            ) as T
            BackgroundPermissionsViewModel::class.java -> BackgroundPermissionsViewModel(
                baseViewModelDependencies,
                appInteractor
            ) as T
            CurrentTripViewModel::class.java -> CurrentTripViewModel(
                baseViewModelDependencies,
                appInteractor
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
