package com.hypertrack.android.use_case.app

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.use_case.login.NotLoggedIn
import com.hypertrack.android.use_case.login.UserLoginStatus
import com.hypertrack.android.use_case.sdk.GetConfiguredHypertrackSdkInstanceUseCase
import com.hypertrack.android.utils.toNullable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class InitAppUseCase(
    private val getConfiguredHypertrackSdkInstanceUseCase: GetConfiguredHypertrackSdkInstanceUseCase,
) {

    fun execute(
        appScopeParam: AppScope
    ): Flow<UserLoginStatus> {
        return { appScopeParam }.asFlow()
            .flatMapConcat { appScope ->
                val publishableKey = appScope
                    .publishableKeyRepository
                    .publishableKey
                    .load()
                    .toNullable()

                if (publishableKey != null) {
                    getConfiguredHypertrackSdkInstanceUseCase.execute(publishableKey)
                        .map { hypertrackSdk ->
                            LoggedIn(hypertrackSdk, publishableKey)
                        }
                } else {
                    flowOf(NotLoggedIn)
                }
            }
    }

}
