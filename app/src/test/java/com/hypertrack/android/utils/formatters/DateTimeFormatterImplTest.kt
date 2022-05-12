package com.hypertrack.android.utils.formatters

import com.hypertrack.android.utils.datetime.toIso
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import java.time.ZonedDateTime

class DateTimeFormatterImplTest {
    companion object {
        fun testDatetimeFormatter(): DateTimeFormatter {
            return mockk() {
                every { formatTime(any()) } answers {
                    firstArg<ZonedDateTime>().toIso()
                }
            }
        }
    }
}
