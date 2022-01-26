package com.hypertrack.android.utils.datetime

import com.hypertrack.android.utils.TypeWrapper
import java.time.ZonedDateTime

interface RangedDateTime {
    val value: ZonedDateTime
}

class StartDateTime(value: ZonedDateTime) : TypeWrapper<ZonedDateTime>(value), RangedDateTime
class EndDateTime(value: ZonedDateTime) : TypeWrapper<ZonedDateTime>(value), RangedDateTime

sealed class DateTimeRange(
    val start: StartDateTime,
) {
    companion object {
        fun create(start: StartDateTime, end: EndDateTime?): DateTimeRange {
            return if (end != null) {
                ClosedDateTimeRange(start, end)
            } else {
                OpenDateTimeRange(start)
            }
        }
    }
}

class OpenDateTimeRange(
    start: StartDateTime,
) : DateTimeRange(
    start,
)

class ClosedDateTimeRange(
    start: StartDateTime,
    val end: EndDateTime
) : DateTimeRange(
    start,
) {
    val duration = timeBetween(start.value, end.value)
}
