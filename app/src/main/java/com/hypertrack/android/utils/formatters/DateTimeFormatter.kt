package com.hypertrack.android.utils.formatters

import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetimeFromString
import com.hypertrack.logistics.android.github.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.abs

interface TimeFormatter {
    fun formatSeconds(seconds: Long): String
    fun formatSeconds(seconds: Int): String
}

interface DatetimeFormatter {
    fun formatTime(dt: ZonedDateTime): String
    fun formatDate(dt: ZonedDateTime): String
    fun formatDate(d: LocalDate): String
    fun formatDatetime(dt: ZonedDateTime): String
}

open class DateTimeFormatterImpl(val zoneId: ZoneId = ZoneId.systemDefault()) : DatetimeFormatter {
    override fun formatTime(dt: ZonedDateTime): String {
        return try {
            dt.withZoneSameInstant(zoneId).toLocalTime().format(
                DateTimeFormatter.ofLocalizedTime(
                    FormatStyle.SHORT
                )
            ).replace(" PM", "pm").replace(" AM", "am")
        } catch (ignored: Exception) {
            dt.format(DateTimeFormatter.ISO_DATE_TIME)
        }
    }

    override fun formatDate(dt: ZonedDateTime): String {
        return dt.withZoneSameInstant(zoneId)
            .format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
    }

    override fun formatDate(d: LocalDate): String {
        return d.format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
    }

    override fun formatDatetime(dt: ZonedDateTime): String {
        val zonedDt = dt.withZoneSameInstant(zoneId)
        val date = zonedDt.format(
            createFormatterWithoutYear(
                FormatStyle.MEDIUM,
                Locale.getDefault()
            )
        )
        val time = zonedDt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return "$date, $time"
    }
}

private fun createFormatterWithoutYear(
    style: FormatStyle,
    locale: Locale
): DateTimeFormatter {
    try {
        var pattern: String = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            style, null, Chronology.ofLocale(locale), locale
        )
        pattern = pattern.replaceFirst("\\P{IsLetter}+[Yy]+".toRegex(), "")
        pattern = pattern.replaceFirst("^[Yy]+\\P{IsLetter}+".toRegex(), "")
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return formatter
    } catch (e: Exception) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
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

class TimeFormatterImpl(val osUtilsProvider: OsUtilsProvider) : TimeFormatter {
    override fun formatSeconds(seconds: Int): String {
        return formatSeconds(seconds.toLong())
    }

    override fun formatSeconds(seconds: Long): String {
        return abs(seconds).let { totalSeconds ->
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) {
                osUtilsProvider.stringFromResource(R.string.duration, hours, minutes)
            } else {
                osUtilsProvider.stringFromResource(R.string.duration_minutes, minutes)
            }
        }
    }
}



