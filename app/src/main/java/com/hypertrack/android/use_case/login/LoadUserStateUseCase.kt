package com.hypertrack.android.use_case.login

import android.content.Context
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.state.HistoryState
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserState
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.use_case.app.CreateUserScopeUseCase
import com.hypertrack.android.use_case.app.SetCrashReportingIdUseCase
import com.hypertrack.android.use_case.app.UserScopeUseCases
import com.hypertrack.android.use_case.sdk.TrackingStarted
import com.hypertrack.android.use_case.sdk.TrackingStopped
import com.hypertrack.android.utils.DeviceInfoUtils
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.crashlytics.LoggedInUserIdentifier
import com.hypertrack.android.utils.crashlytics.NotLoggedInUserIdentifier
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.mapSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoadUserStateUseCase(
    private val context: Context,
    private val createUserScopeUseCase: CreateUserScopeUseCase,
    private val setCrashReportingIdUseCase: SetCrashReportingIdUseCase,
    private val loadUserDataUseCase: LoadUserDataUseCase,
) {

    // must be executed only on main thread
    // required by UserScope.TripInteractor
    fun execute(
        userLoginStatus: UserLoginStatus,
    ): Flow<Result<out UserState>> {
        return when (userLoginStatus) {
            is LoggedIn -> {
                createUserScopeUseCase.execute(
                    userLoginStatus.hyperTrackSdk,
                    userLoginStatus.publishableKey,
                ).zip(loadUserDataUseCase.execute()) { userScope, userDataResult ->
                    when (userDataResult) {
                        is Success -> {
                            val initialTrackingState =
                                if (userScope.hyperTrackService.isServiceRunning) {
                                    TrackingStarted
                                } else {
                                    TrackingStopped
                                }
                            Injector.crashReportsProvider.log(
                                "initial_tracking_state $initialTrackingState"
                            )
                            UserLoggedIn(
                                deviceId = DeviceId(userScope.hyperTrackService.deviceId),
                                userData = userDataResult.data,
                                userScope = userScope,
                                trackingState = initialTrackingState,
                                userLocation = userScope.deviceLocationProvider.deviceLocation.value,
                                history = HistoryState(
                                    lastTodayReload = ZonedDateTime.ofInstant(
                                        Instant.EPOCH,
                                        ZoneId.systemDefault()
                                    ),
                                    days = mapOf()
                                ),
                                useCases = UserScopeUseCases(
                                    userScope.appInteractor,
                                    userScope.appScope,
                                    userScope
                                )
                            ).asSuccess()
                        }
                        is Failure -> {
                            Failure(userDataResult.exception)
                        }
                    }
                }.flatMapSuccess { userLoggedIn ->
                    setCrashReportingIdUseCase.execute(
                        LoggedInUserIdentifier(userLoggedIn.deviceId)
                    ).map { userLoggedIn.asSuccess() }
                }
            }
            NotLoggedIn -> {
                flowOf(DeviceInfoUtils.getHardwareId(context)).flatMapSuccess { hardwareId ->
                    setCrashReportingIdUseCase.execute(
                        NotLoggedInUserIdentifier(hardwareId)
                    )
                }.mapSuccess {
                    UserNotLoggedIn
                }
            }
        }
    }

}
