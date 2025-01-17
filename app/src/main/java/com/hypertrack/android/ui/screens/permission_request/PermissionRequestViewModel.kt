package com.hypertrack.android.ui.screens.permission_request

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.exception.IllegalActionException

class PermissionRequestViewModel(
    baseDependencies: BaseViewModelDependencies,
) : BaseViewModel(baseDependencies) {

    val showPermissionsButton = MutableLiveData(true)
    val showSkipButton = MutableLiveData(false)

    fun handleAction(action: Action) {
        appInteractor.appState.requireValue().let { state ->
            when (state) {
                is AppInitialized -> {
                    when (state.userState) {
                        is UserLoggedIn -> {
                            handleActionOnLoggedIn(action, state.userState.userScope)
                        }
                        UserNotLoggedIn -> {
                            crashReportsProvider.logException(IllegalActionException(action, state))
                        }
                    }
                }
                is AppNotInitialized -> {
                    crashReportsProvider.logException(IllegalActionException(action, state))
                }
            }
        }

    }

    private fun handleActionOnLoggedIn(action: Action, userScope: UserScope) {
        when (action) {
            is OnResumeAction -> {
                onPermissionResult(
                    action.activity,
                    userScope.permissionsInteractor,
                    userScope.hyperTrackService
                )
            }
            OnSkipClickedAction -> {
                if (userScope.permissionsInteractor.isBackgroundLocationGranted()) {
                    destination.postValue(PermissionRequestFragmentDirections.actionGlobalVisitManagementFragment())
                } else {
                    destination.postValue(PermissionRequestFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                }
            }
            is RequestPermissionsAction -> {
                userScope.permissionsInteractor.requestRequiredPermissions(action.activity)
            }
        } as Any?
    }

    private fun onPermissionResult(
        activity: Activity,
        permissionsInteractor: PermissionsInteractor,
        hyperTrackService: HyperTrackService
    ) {
        permissionsInteractor.checkPermissionsState().let {
            when (it.getNextPermissionRequest()) {
                PermissionDestination.FOREGROUND_AND_TRACKING -> {
                    showPermissionsButton.postValue(true)
                }
                PermissionDestination.BACKGROUND -> {
                    hyperTrackService.syncDeviceSettings()
                    destination.postValue(PermissionRequestFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                }
                PermissionDestination.PASS -> {
                    hyperTrackService.syncDeviceSettings()
                    destination.postValue(PermissionRequestFragmentDirections.actionGlobalVisitManagementFragment())
                }
            }

            showPermissionsButton.postValue(!permissionsInteractor.isBasePermissionsGranted())
            showSkipButton.postValue(permissionsInteractor.isBasePermissionsGranted())
        }
    }

}
