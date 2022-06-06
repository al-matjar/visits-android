package com.hypertrack.android.use_case.navigation

import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.use_case.permissions.AllGranted
import com.hypertrack.android.use_case.permissions.BackgroundLocationNotGranted
import com.hypertrack.android.use_case.permissions.CheckForPermissionsUseCase
import com.hypertrack.android.use_case.permissions.LocationOrActivityNotGranted
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
// todo check for permissions in appState
class NavigateToUserScopeScreensUseCase {

    fun execute(
        permissionsInteractor: PermissionsInteractor
    ): Flow<Result<NavDirections>> {
        return CheckForPermissionsUseCase(
            permissionsInteractor
        ).execute().map { result ->
            result.map {
                when (it) {
                    AllGranted -> VisitsManagement
                    LocationOrActivityNotGranted -> RequestPermissions
                    BackgroundLocationNotGranted -> RequestBackgroundLocation
                }
            }
        }.mapSuccess {
            when (it) {
                VisitsManagement -> {
                    NavGraphDirections.actionGlobalVisitManagementFragment()
                }
                RequestPermissions -> {
                    NavGraphDirections.actionGlobalPermissionRequestFragment()
                }
                RequestBackgroundLocation -> {
                    NavGraphDirections.actionGlobalBackgroundPermissionsFragment()
                }
            }
        }
    }

}
