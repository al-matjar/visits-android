package com.hypertrack.android.utils.datetime

import com.hypertrack.android.utils.TimeValue
import com.hypertrack.android.utils.formatters.createFormatterWithoutYear
import com.hypertrack.android.utils.toSeconds
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*

// use this function because ZonedDateTime.parse() doesn't enforce compile time non-nullability
// should be used only in models (each model should provide parsed datetime)
fun dateTimeFromString(str: String): ZonedDateTime {
    return ZonedDateTime.parse(str)
}

fun timeBetween(
    startDatetime: ZonedDateTime,
    endDatetime: ZonedDateTime
): TimeValue {
    return ChronoUnit.SECONDS.between(startDatetime, endDatetime).let {
        if (it >= 0) it else 0
    }.toSeconds()
}

fun LocalDate.prettyFormat(): String {
    return format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
}

fun ZonedDateTime.prettyFormatDate(): String {
    return format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
}

fun ZonedDateTime.toIso(): String {
    return withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME)
        .replace("[UTC]", "")
}


