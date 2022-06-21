package com.hypertrack.android.use_case.app

import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class LogExceptionToCrashlyticsUseCaseTest {

    companion object {
        fun logExceptionToCrashlyticsUseCase(): LogExceptionToCrashlyticsUseCase {
            return LogExceptionToCrashlyticsUseCase(
                crashReportsProvider()
            )
        }

        fun stubLogExceptionToCrashlyticsUseCase(): LogExceptionToCrashlyticsUseCase {
            return mockk {
                every { execute(any(), any()) } returns flowOf(Unit)
            }
        }
    }


}
