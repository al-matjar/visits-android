package com.hypertrack.android.utils

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase

class FirebaseCrashReportsProviderTest {
    companion object {
        fun crashReportsProvider(): CrashReportsProvider {
            return mockk {
                every { logException(any<Exception>(), any()) } answers {
                    println(firstArg<Exception>().format())
                    throw firstArg()
                }
                every { log(any()) } answers {
                    println(firstArg<String>().format())
                }
            }
        }
    }
}
