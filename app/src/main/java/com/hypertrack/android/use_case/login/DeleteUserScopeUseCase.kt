package com.hypertrack.android.use_case.login

import com.hypertrack.android.data.AccountDataStorage
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.models.Unspecified
import com.hypertrack.android.repository.AccountData
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.MyPreferences
import com.hypertrack.android.repository.PublishableKeyRepository
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.repository.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat

@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class DeleteUserScopeUseCase(
    private val deleteUserScopeDataUseCase: DeleteUserScopeDataUseCase
) {

    fun execute(userScope: UserScope): Flow<Unit> {
        return {
            userScope.hyperTrackService.stopTracking()
        }.asFlow().flatMapConcat {
            deleteUserScopeDataUseCase.execute()
        }
    }

}
