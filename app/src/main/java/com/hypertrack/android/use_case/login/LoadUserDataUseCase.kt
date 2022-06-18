package com.hypertrack.android.use_case.login

import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleException
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.toFlow
import kotlinx.coroutines.flow.Flow
import java.lang.NullPointerException

@Suppress("OPT_IN_USAGE")
class LoadUserDataUseCase(
    private val userRepository: UserRepository
) {

    fun execute(): Flow<Result<UserData>> {
        return (userRepository.userData.load()).toFlow()
            .flatMapSuccess {
                it?.asSuccess()?.toFlow()
                    ?: SimpleException(
                        "Failed to load user data for LoggedIn state. Please clear the app data or reinstall the app"
                    ).asFailure<UserData>().toFlow()
            }
    }

}
