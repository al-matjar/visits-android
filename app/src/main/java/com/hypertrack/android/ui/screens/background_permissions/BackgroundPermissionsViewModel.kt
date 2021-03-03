package com.hypertrack.android.ui.screens.background_permissions

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.ui.MainActivity

class BackgroundPermissionsViewModel(
        private val permissionsInteractor: PermissionsInteractor
) : ViewModel() {

    val destination = MutableLiveData<NavDirections>()

    fun onAllowClick(activity: Activity) {
        permissionsInteractor.requestBackgroundLocationPermission(activity)
    }

    fun onPermissionResult(activity: Activity) {
        when (permissionsInteractor.checkPermissionState(activity).getDestination()) {
            PermissionDestination.PASS -> {
                destination.postValue(BackgroundPermissionsFragmentDirections.actionBackgroundPermissionsFragmentToPermissionRequestFragment())
            }
            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                destination.postValue(BackgroundPermissionsFragmentDirections.actionBackgroundPermissionsFragmentToVisitManagementFragment())
            }
            PermissionDestination.BACKGROUND -> {
                //todo should we let user proceed?
            }
            PermissionDestination.WHITELISTING -> {
                destination.postValue(BackgroundPermissionsFragmentDirections.actionBackgroundPermissionsFragmentToPermissionRequestFragment())
            }
        }

    }

}