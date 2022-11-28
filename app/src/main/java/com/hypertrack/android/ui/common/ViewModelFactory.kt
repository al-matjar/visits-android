package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.optics.AppStateOptics
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.screens.background_permissions.BackgroundPermissionsViewModel
import com.hypertrack.android.ui.screens.confirm_email.ConfirmEmailViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.BaseHistoryStyle
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewModel
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.mapState

// todo set separate factories for all vms
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val useCases: UseCases
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseViewModelDependencies = BaseViewModelDependencies(
            appInteractor,
            appScope.osUtilsProvider,
            appScope.osUtilsProvider,
            appScope.crashReportsProvider,
            appScope.actionsScope,
            appScope.effectsScope
        )
        return when (modelClass) {
            ConfirmEmailViewModel::class.java -> ConfirmEmailViewModel(
                baseViewModelDependencies,
                useCases.verifyByOtpCodeUseCase,
                useCases.resendExceptionToCrashlyticsUseCase,
                useCases.loadUserStateAfterSignInUseCase,
            ) as T
            SignInViewModel::class.java -> SignInViewModel(
                baseViewModelDependencies,
                useCases.signInUseCase,
                useCases.getBranchDataFromAppBackendUseCase,
                useCases.loginWithDeeplinkParamsUseCase,
                useCases.logExceptionToCrashlyticsUseCase,
                useCases.logMessageToCrashlyticsUseCase,
                useCases.validateDeeplinkUrlUseCase,
                appScope.moshi
            ) as T
            VisitsManagementViewModel::class.java -> VisitsManagementViewModel(
                baseViewModelDependencies,
                appInteractor.appState,
                appScope.preferencesRepository,
            ) as T
            PermissionRequestViewModel::class.java -> PermissionRequestViewModel(
                baseViewModelDependencies,
            ) as T
            HistoryViewModel::class.java -> HistoryViewModel(
                baseViewModelDependencies,
                appInteractor.appStateFlow.mapState(appScope.actionsScope) {
                    AppStateOptics.getHistoryViewState(it)
                },
                appScope.dateTimeFormatter,
                appScope.timeFormatter,
                appScope.distanceFormatter,
                BaseHistoryStyle(appScope.appContext)
            ) as T
            BackgroundPermissionsViewModel::class.java -> BackgroundPermissionsViewModel(
                baseViewModelDependencies,
            ) as T
            CurrentTripViewModel::class.java -> CurrentTripViewModel(
                baseViewModelDependencies,
                MapUiEffectHandler(appInteractor),
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
