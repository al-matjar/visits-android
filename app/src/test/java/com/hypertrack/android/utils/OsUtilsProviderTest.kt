package com.hypertrack.android.utils

import com.hypertrack.android.use_case.HandlePushUseCaseTest
import io.mockk.mockk
import junit.framework.TestCase

class OsUtilsProviderTest {
    companion object {
        fun resourceProvider(): ResourceProvider {
            return object :
                ResourceProvider by mockk<OsUtilsProvider>(relaxed = true) {
                override fun stringFromResource(res: Int, vararg formatArgs: Any): String {
                    return listOf(res, formatArgs.joinToString(" ")).joinToString(" ")
                }

                override fun stringFromResource(res: Int): String {
                    return res.toString()
                }
            }
        }
    }
}
