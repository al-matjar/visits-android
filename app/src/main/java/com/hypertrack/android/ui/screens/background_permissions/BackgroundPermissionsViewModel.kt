package com.hypertrack.android.ui.screens.background_permissions

import android.app.Activity
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import kotlinx.coroutines.FlowPreview

@FlowPreview
class BackgroundPermissionsViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val permissionsInteractor: PermissionsInteractor
) : BaseViewModel(baseDependencies) {

    fun onAllowClick(activity: Activity) {
        permissionsInteractor.requestBackgroundLocationPermission(activity)
    }

    fun onPermissionResult(activity: Activity) {
        when (permissionsInteractor.checkPermissionsState().getNextPermissionRequest()) {
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

