package com.hypertrack.android.utils.datetime

import java.time.ZonedDateTime

interface RangedDateTime {
    val value: ZonedDateTime
}

data class StartDateTime(override val value: ZonedDateTime) : RangedDateTime
data class EndDateTime(override val value: ZonedDateTime) : RangedDateTime

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
