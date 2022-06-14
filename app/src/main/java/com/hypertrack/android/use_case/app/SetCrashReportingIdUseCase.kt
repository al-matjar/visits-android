package com.hypertrack.android.use_case.app

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.crashlytics.LoggedInUserIdentifier
import com.hypertrack.android.utils.crashlytics.NotLoggedInUserIdentifier
import com.hypertrack.android.utils.crashlytics.UserIdentifier
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("OPT_IN_USAGE")
class SetCrashReportingIdUseCase(
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi
) {

    fun execute(userIdentifier: UserIdentifier): Flow<Result<Unit>> {
        return tryAsResult {
            when (userIdentifier) {
                is LoggedInUserIdentifier -> {
                    moshi.adapter(LoggedInUserIdentifier::class.java).toJson(userIdentifier)
                }
                is NotLoggedInUserIdentifier -> {
                    moshi.adapter(NotLoggedInUserIdentifier::class.java).toJson(userIdentifier)
                }
            }.let {
                crashReportsProvider.setUserIdentifier(it)
            }
        }.toFlow()
    }

}

