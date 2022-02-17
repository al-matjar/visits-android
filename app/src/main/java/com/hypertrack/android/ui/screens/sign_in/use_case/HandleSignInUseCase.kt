package com.hypertrack.android.ui.screens.sign_in.use_case

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.SignedInAction
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.screens.sign_in.SignInFragmentDirections
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.tryAsSimpleResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class HandleSignInUseCase(
    private val appInteractor: AppInteractor,
    private val destination: MutableLiveData<Consumable<NavDirections>>
) {

    fun execute(userState: UserLoggedIn): Flow<SimpleResult> {
        return {
            appInteractor.handleAction(SignedInAction(userState))

            tryAsSimpleResult {
                when (userState.userScope.permissionsInteractor.checkPermissionsState()
                    .getNextPermissionRequest()) {
                    PermissionDestination.PASS -> {
                        destination.postValue(SignInFragmentDirections.actionGlobalVisitManagementFragment())
                    }
                    PermissionDestination.FOREGROUND_AND_TRACKING -> {
                        destination.postValue(SignInFragmentDirections.actionGlobalPermissionRequestFragment())
                    }
                    PermissionDestination.BACKGROUND -> {
                        destination.postValue(SignInFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                    }
                }
            }
        }.asFlow()
    }

}
