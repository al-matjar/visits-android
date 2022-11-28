package com.hypertrack.android.use_case.login

import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.LoginAppAction
import com.hypertrack.android.interactors.app.action.LoginAction
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.PublishableKeyRepository
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.mapSimpleToSuccess
import com.hypertrack.android.utils.toFlow
import com.hypertrack.sdk.HyperTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class LoginWithPublishableKeyUseCase(
    private val refreshUserAccessTokenUseCase: RefreshUserAccessTokenUseCase,
    private val userRepository: UserRepository,
    private val accessTokenRepository: AccessTokenRepository,
    private val publishableKeyRepository: PublishableKeyRepository,
) {

    fun execute(
        hypertrackSdk: HyperTrack,
        userData: UserData,
        publishableKey: RealPublishableKey,
        deeplinkWithoutGetParams: String?
    ): Flow<Result<out LoggedIn>> {
        return refreshUserAccessTokenUseCase.execute(
            publishableKey,
            DeviceId(hypertrackSdk.deviceID)
        ).flatMapSuccess { accessTokenResult ->
            // publishable key is valid
            accessTokenRepository.accessToken = accessTokenResult
            publishableKeyRepository.publishableKey.save(publishableKey)

            SaveUserDataUseCase(
                hypertrackSdk,
                userRepository,
            ).execute(userData, deeplinkWithoutGetParams)
                .mapSimpleToSuccess {
                    LoggedIn(hypertrackSdk, publishableKey)
                }
        }
    }

}

