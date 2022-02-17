package com.hypertrack.android.utils

import io.mockk.mockk

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
