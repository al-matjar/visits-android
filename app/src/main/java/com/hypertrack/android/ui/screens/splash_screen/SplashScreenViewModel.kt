package com.hypertrack.android.ui.screens.splash_screen

import androidx.lifecycle.viewModelScope
import com.hypertrack.android.delegates.DeeplinkResultDelegate
import com.hypertrack.android.interactors.*
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.utils.HyperTrackService
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val deeplinkInteractor: DeeplinkInteractor,
    private val permissionsInteractor: PermissionsInteractor,
    private val hyperTrackService: () -> HyperTrackService?
) : BaseViewModel(baseDependencies) {

    private val deeplinkDelegate = object : DeeplinkResultDelegate(
        deeplinkInteractor,
        crashReportsProvider,
        resourceProvider,
        errorHandler
    ) {
        override fun proceedToSignIn(handleDeeplinkResult: HandleDeeplinkResult) {
            destination.postValue(
                SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment()
            )
        }

        override fun proceedToVisitsManagement() {
            when (permissionsInteractor.checkPermissionsState().getNextPermissionRequest()) {
                PermissionDestination.PASS -> {
                    destination.postValue(SplashScreenFragmentDirections.actionGlobalVisitManagementFragment())
                }
                PermissionDestination.FOREGROUND_AND_TRACKING -> {
                    destination.postValue(SplashScreenFragmentDirections.actionGlobalPermissionRequestFragment())
                }
                PermissionDestination.BACKGROUND -> {
                    destination.postValue(SplashScreenFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                }
            }
        }
    }

    fun handleDeeplink(result: DeeplinkResult, activity: MainActivity) {
        loadingState.postValue(true)
        viewModelScope.launch {
            deeplinkDelegate.handleDeeplink(result, activity)
            loadingState.postValue(false)
        }
    }

    fun activityOnResume() {
        hyperTrackService.invoke()?.showPermissionsPrompt()
    }

}


