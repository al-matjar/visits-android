package com.hypertrack.android.use_case.login

import android.os.Build.VERSION_CODES.P
import androidx.lifecycle.Transformations.map
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserState
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.use_case.app.CreateUserScopeUseCase
import com.hypertrack.android.use_case.sdk.TrackingStarted
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import com.hypertrack.android.use_case.sdk.TrackingStopped
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.TrackingState
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoadUserStateUseCase(
    private val createUserScopeUseCase: CreateUserScopeUseCase,
    private val loadUserDataUseCase: LoadUserDataUseCase,
    private val trackingState: TrackingState,
) {

    // must be executed only on main thread
    // required by UserScope.TripInteractor
    fun execute(
        userLoginStatus: UserLoginStatus,
    ): Flow<Result<UserState>> {
        return when (userLoginStatus) {
            is LoggedIn -> {
                createUserScopeUseCase.execute(
                    userLoginStatus.hyperTrackSdk,
                    userLoginStatus.publishableKey,
                    trackingState
                ).zip(loadUserDataUseCase.execute()) { userScope, userDataResult ->
                    when (userDataResult) {
                        is Success -> {
                            val initialTrackingState =
                                if (userScope.hyperTrackService.isServiceRunning) {
                                    TrackingStarted
                                } else {
                                    TrackingStopped
                                }
                            UserLoggedIn(
                                deviceId = DeviceId(userScope.hyperTrackService.deviceId),
                                userData = userDataResult.data,
                                userScope = userScope,
                                trackingState = initialTrackingState
                            ).asSuccess()
                        }
                        is Failure -> {
                            Failure(userDataResult.exception)
                        }
                    }
                }
            }
            NotLoggedIn -> {
                flowOf(UserNotLoggedIn.asSuccess())
            }
        }
    }

}
