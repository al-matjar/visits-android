package com.hypertrack.android.ui.screens.background_permissions

import android.app.Activity
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.utils.exception.IllegalActionException

class BackgroundPermissionsViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
) : BaseViewModel(baseDependencies) {

    fun handleAction(action: Action) {
        appInteractor.appState.requireValue().let { state ->
            when (state) {
                is Initialized -> {
                    when (state.userState) {
                        is UserLoggedIn -> {
                            handleActionOnLoggedIn(action, state.userState.userScope)
                        }
                        UserNotLoggedIn -> {
                            crashReportsProvider.logException(IllegalActionException(action, state))
                        }
                    }
                }
                is NotInitialized -> {
                    crashReportsProvider.logException(IllegalActionException(action, state))
                }
            }
        }

    }

    private fun handleActionOnLoggedIn(action: Action, userScope: UserScope) {
        when (action) {
            is OnAllowClick -> {
                userScope.permissionsInteractor.requestBackgroundLocationPermission(action.activity)
            }
            is OnPermissionsResult -> {
                onPermissionResult(action.activity, userScope)
            }
        } as Any?
    }

    private fun onPermissionResult(activity: Activity, userScope: UserScope) {
        when (userScope.permissionsInteractor.checkPermissionsState().getNextPermissionRequest()) {
            PermissionDestination.PASS -> {
                destination.postValue(BackgroundPermissionsFragmentDirections.actionGlobalVisitManagementFragment())
            }
            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                destination.postValue(BackgroundPermissionsFragmentDirections.actionGlobalPermissionRequestFragment())
            }
            PermissionDestination.BACKGROUND -> {
                //todo request again
                //note that permissions activity may not appear and granting can be impossible
                destination.postValue(BackgroundPermissionsFragmentDirections.actionGlobalVisitManagementFragment())
            }
        }
    }

}

