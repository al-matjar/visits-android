package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class RefreshUserAccessTokenUseCase(
    private val accessTokenRepository: AccessTokenRepository
) {

    fun execute(publishableKey: PublishableKey, deviceId: DeviceId): Flow<Result<UserAccessToken>> {
        return {
            accessTokenRepository.refreshToken(publishableKey, deviceId)
        }.asFlow()
    }

}
