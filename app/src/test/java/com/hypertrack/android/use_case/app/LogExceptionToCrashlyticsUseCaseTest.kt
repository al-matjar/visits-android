package com.hypertrack.android.use_case.app

import android.os.AsyncTask.execute
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

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
