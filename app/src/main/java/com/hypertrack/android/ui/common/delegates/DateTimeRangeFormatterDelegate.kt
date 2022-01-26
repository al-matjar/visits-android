package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.utils.datetime.DateTimeRange
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.ClosedDateTimeRange
import com.hypertrack.android.utils.datetime.OpenDateTimeRange
import com.hypertrack.android.utils.datetime.RangedDateTime
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.logistics.android.github.R
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeRangeFormatterDelegate(
    private val osUtilsProvider: OsUtilsProvider,
    private val dateTimeFormatter: DateTimeFormatter
) {

    fun formatTimeRange(dateTimeRange: DateTimeRange): String {
        return when (dateTimeRange) {
            is ClosedDateTimeRange -> "${
                getTimeString(dateTimeRange.start)
            } — ${
                getTimeString(dateTimeRange.end)
            }"
            is OpenDateTimeRange -> "${
                getTimeString(dateTimeRange.start)
            } — ${
                osUtilsProvider.stringFromResource(R.string.now)
            }"
        }
    }

    fun formatDatetimeRange(
        dateTimeRange: DateTimeRange
    ): String {
        val enterDt = dateTimeRange.start
        val exitDt = when (dateTimeRange) {
            is ClosedDateTimeRange -> dateTimeRange.end
            is OpenDateTimeRange -> null
        }

        val equalDay = enterDt.value.dayOfMonth == exitDt?.value?.dayOfMonth

        return if (equalDay) {
            "${
                getDateString(enterDt)
            }, ${
                getTimeString(enterDt)
            } — ${getTimeString(exitDt)}"
        } else {
            "${
                getDateString(enterDt)
            }, ${
                getTimeString(enterDt)
            } — ${
                exitDt?.let {
                    "${
                        getDateString(exitDt)
                    }, ${
                        getTimeString(exitDt)
                    }"
                } ?: osUtilsProvider.stringFromResource(R.string.now)
            }"
        }
    }

    private fun getDateString(datetime: RangedDateTime): String {
        datetime.value.let {
            val now = ZonedDateTime.now()
            val yesterday = ZonedDateTime.now().minusDays(1)
            return when {
                isSameDay(it, now) -> {
                    osUtilsProvider.stringFromResource(R.string.place_today)
                }
                isSameDay(it, yesterday) -> {
                    osUtilsProvider.stringFromResource(R.string.place_yesterday)
                }
                else -> {
                    dateTimeFormatter.formatDate(it)
                }
            }
        }
    }

    private fun getTimeString(it: RangedDateTime?): String {
        return it?.let {
            dateTimeFormatter.formatTime(it.value)
        } ?: osUtilsProvider.stringFromResource(R.string.now)
    }

    private fun isSameDay(dateTime1: ZonedDateTime, dateTime2: ZonedDateTime): Boolean {
        val utcZoneId = ZoneId.of("UTC")
        val utcDateTime1 = dateTime1.withZoneSameInstant(utcZoneId)
        val utcDateTime2 = dateTime2.withZoneSameInstant(utcZoneId)
        return utcDateTime1.dayOfYear == utcDateTime2.dayOfYear
                && utcDateTime1.year == utcDateTime2.year
    }

}
