package com.hypertrack.android.utils.formatters

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

interface DateTimeFormatter {
    fun formatTime(dt: ZonedDateTime): String
    fun formatDate(dt: ZonedDateTime): String
    fun formatDate(d: LocalDate): String
    fun formatDateTime(dt: ZonedDateTime): String
}

open class DateTimeFormatterImpl(val zoneId: ZoneId = ZoneId.systemDefault()) :
    com.hypertrack.android.utils.formatters.DateTimeFormatter {
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

    override fun formatDateTime(dt: ZonedDateTime): String {
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

fun createFormatterWithoutYear(
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
