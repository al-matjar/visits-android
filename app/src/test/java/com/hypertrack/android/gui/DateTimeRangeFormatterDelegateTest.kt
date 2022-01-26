package com.hypertrack.android.gui


import com.hypertrack.android.ui.common.delegates.DateTimeRangeFormatterDelegate
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.DateTimeRange
import com.hypertrack.android.utils.datetime.EndDateTime
import com.hypertrack.android.utils.datetime.StartDateTime
import com.hypertrack.android.utils.datetime.prettyFormatDate
import com.hypertrack.android.utils.formatters.DateTimeFormatterImpl

import com.hypertrack.logistics.android.github.R
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DateTimeRangeFormatterDelegateTest {

    @Test
    fun `it should properly format time for range`() {
        val osUtilsProvider = mockk<OsUtilsProvider>() {
            every { stringFromResource(R.string.place_today) } returns "Today"
            every { stringFromResource(R.string.place_yesterday) } returns "Yesterday"
        }
        val formatter: com.hypertrack.android.utils.formatters.DateTimeFormatter =
            object : DateTimeFormatterImpl(ZoneId.of("UTC")) {
                override fun formatTime(dt: ZonedDateTime): String {
                    return dt.format(DateTimeFormatter.ISO_DATE_TIME).replace("[UTC]", "")
                }
            }
        val delegate = DateTimeRangeFormatterDelegate(
            osUtilsProvider,
            formatter
        )

        test(
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            delegate::formatTimeRange
        ) { res, startDatetime, endDatetime ->
            println(res)
            val expected =
                "${formatter.formatTime(startDatetime)} — ${formatter.formatTime(endDatetime)}"
            println(expected)
            assertEquals(expected, res)
        }
    }

    @Test
    fun `it should properly format datetime for range`() {
        val osUtilsProvider = mockk<OsUtilsProvider>() {
            every { stringFromResource(R.string.place_today) } returns "Today"
            every { stringFromResource(R.string.place_yesterday) } returns "Yesterday"
        }
        val formatter: com.hypertrack.android.utils.formatters.DateTimeFormatter =
            object : DateTimeFormatterImpl(ZoneId.of("UTC")) {
                override fun formatTime(dt: ZonedDateTime): String {
                    return dt.format(DateTimeFormatter.ISO_DATE_TIME).replace("[UTC]", "")
                }
            }
        val delegate = DateTimeRangeFormatterDelegate(
            osUtilsProvider,
            formatter
        )

        val today = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
            .withHour(13).withMinute(1)
        val yesterday = today.minusDays(1)
        val weekAgo = today.minusDays(7)
        val longTimeAgo = today.minusDays(14)

        test(today, today, delegate::formatDatetimeRange) { res, startDatetime, endDatetime ->
            println(res)
            val expected =
                "Today, ${formatter.formatTime(startDatetime)} — ${formatter.formatTime(endDatetime)}"
            println(expected)
            assertEquals(expected, res)
        }

        test(
            yesterday,
            yesterday,
            delegate::formatDatetimeRange
        ) { res, startDatetime, endDatetime ->
            assertEquals(
                "Yesterday, ${formatter.formatTime(startDatetime)} — ${
                    formatter.formatTime(
                        endDatetime
                    )
                }",
                res
            )
        }

        test(yesterday, today, delegate::formatDatetimeRange) { res, startDatetime, endDatetime ->
            assertEquals(
                "Yesterday, ${formatter.formatTime(startDatetime)} — Today, ${
                    formatter.formatTime(
                        endDatetime
                    )
                }", res
            )
        }

        test(weekAgo, yesterday, delegate::formatDatetimeRange) { res, startDatetime, endDatetime ->
            assertEquals(
                "${weekAgo.prettyFormatDate()}, ${formatter.formatTime(startDatetime)} — Yesterday, ${
                    formatter.formatTime(
                        endDatetime
                    )
                }", res
            )
        }

        test(weekAgo, weekAgo, delegate::formatDatetimeRange) { res, startDatetime, endDatetime ->
            assertEquals(
                "${weekAgo.prettyFormatDate()}, ${formatter.formatTime(startDatetime)} — ${
                    formatter.formatTime(
                        endDatetime
                    )
                }", res
            )
        }

        test(
            longTimeAgo,
            weekAgo,
            delegate::formatDatetimeRange
        ) { res, startDatetime, endDatetime ->
            assertEquals(
                "${longTimeAgo.prettyFormatDate()}, ${formatter.formatTime(startDatetime)} — ${weekAgo.prettyFormatDate()}, ${
                    formatter.formatTime(
                        endDatetime
                    )
                }", res
            )
        }
    }

    fun test(
        startDatetime: ZonedDateTime,
        baseEndDatetime: ZonedDateTime,
        testFunction: (DateTimeRange) -> String,
        checks: (res: String, startDatetime: ZonedDateTime, endDatetime: ZonedDateTime) -> Unit,
    ) {
        val endDatetime = baseEndDatetime.withMinute(30).withSecond(1)

        DateTimeRange.create(StartDateTime(startDatetime), EndDateTime(endDatetime)).let {
            testFunction.invoke(it)
        }.let {
            checks.invoke(
                it,
                startDatetime,
                endDatetime
            )
        }
    }

}
