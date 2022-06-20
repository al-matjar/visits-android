package com.hypertrack.android.use_case.login

import com.hypertrack.android.data.AccountDataStorage
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

@Suppress("OPT_IN_USAGE")
class DeleteUserScopeDataUseCase(
    private val userRepository: UserRepository,
    private val publishableKeyRepository: PublishableKeyRepository,
    private val measurementUnitsRepository: MeasurementUnitsRepository,
    private val accessTokenRepository: AccessTokenRepository,
    private val accountDataSource: AccountDataStorage,
) {

    // todo clear loading photos
    fun execute(): Flow<Unit> {
        return {
            accountDataSource.saveAccountData(AccountData(null))
            accessTokenRepository.accessToken = null
            publishableKeyRepository.publishableKey.save(null)
            userRepository.userData.save(null)
            measurementUnitsRepository.setMeasurementUnits(Unspecified)
        }.asFlow()
    }

}
