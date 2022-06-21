package com.hypertrack.android.use_case.app

import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.utils.toFlow
import io.mockk.every
import io.mockk.mockk

class LogMessageToCrashlyticsUseCaseTest {

    companion object {
        fun testLogMessageToCrashlyticsUseCase(): LogMessageToCrashlyticsUseCase {
            return mockk() {
                every { execute(any()) } returns Unit.toFlow()
            }
        }
    }
}
