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
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class LoadUserDataUseCase(
    private val userRepository: UserRepository,
    private val deleteUserScopeDataUseCase: DeleteUserScopeDataUseCase
) {

    @Suppress("IfThenToElvis")
    fun execute(): Flow<Result<UserData>> {
        return (userRepository.userData.load()).toFlow()
            .flatMapSuccess { userData ->
                if (userData != null) {
                    userData.asSuccess().toFlow()
                } else {
                    deleteUserScopeDataUseCase.execute().map {
                        SimpleException(
                            "Failed to load user data. Please log in again"
                        ).asFailure()
                    }
                }
            }
    }

}
