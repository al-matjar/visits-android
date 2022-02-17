package com.hypertrack.android.use_case.login

import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.mapSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LoadUserStateAfterSignInUseCase(
    private val loadUserStateUseCase: LoadUserStateUseCase,
) {

    // must be executed only on main thread
    // required by UserScope.TripInteractor
    fun execute(
        userLoginStatus: LoggedIn,
    ): Flow<Result<UserLoggedIn>> {
        return flowOf(userLoginStatus).flatMapConcat {
            loadUserStateUseCase.execute(it)
        }.map { result ->
            result.flatMap { userState ->
                when (userState) {
                    is UserLoggedIn -> {
                        userState.asSuccess()
                    }
                    UserNotLoggedIn -> {
                        IllegalStateException(
                            "user must be logged in in this state"
                        ).asFailure()
                    }
                }
            }
        }
    }

}
