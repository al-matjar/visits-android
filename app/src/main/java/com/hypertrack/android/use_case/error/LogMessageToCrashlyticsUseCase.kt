package com.hypertrack.android.use_case.error

import com.hypertrack.android.utils.CrashReportsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("OPT_IN_USAGE")
class LogMessageToCrashlyticsUseCase(
    private val crashReportsProvider: CrashReportsProvider
) {

    fun execute(text: String): Flow<Unit> {
        return {
            crashReportsProvider.log(text)
        }.asFlow()
    }

}
