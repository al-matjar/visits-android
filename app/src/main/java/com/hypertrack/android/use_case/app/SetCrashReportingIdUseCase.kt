package com.hypertrack.android.use_case.app

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.UserIdentifier
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("OPT_IN_USAGE")
class SetCrashReportingIdUseCase(
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi
) {

    fun execute(deviceId: DeviceId): Flow<Unit> {
        return {
            crashReportsProvider.setUserIdentifier(
                moshi.adapter(
                    UserIdentifier::
                    class.java
                ).toJson(
                    UserIdentifier(
                        deviceId = deviceId.value,
                    )
                )
            )
        }.asFlow()
    }

}
