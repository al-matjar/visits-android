package com.hypertrack.android.utils.formatters

import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.PositiveSeconds
import com.hypertrack.android.utils.TimeValue
import com.hypertrack.android.utils.TimeValue.Companion.MINUTES_IN_HOUR
import com.hypertrack.android.utils.TimeValue.Companion.SECONDS_IN_MINUTE
import com.hypertrack.android.utils.toSeconds
import com.hypertrack.logistics.android.github.R
import kotlin.math.abs

interface TimeValueFormatter {
    fun formatTimeValue(timeValue: TimeValue): String

    //todo remove legacy
    fun formatSeconds(seconds: Long): String
    fun formatSeconds(seconds: Int): String
}

class TimeValueFormatterImpl(val osUtilsProvider: OsUtilsProvider) : TimeValueFormatter {
    override fun formatTimeValue(timeValue: TimeValue): String {
        return PositiveSeconds(timeValue).let { positiveSeconds ->
            if (positiveSeconds.hours > 0) {
                osUtilsProvider.stringFromResource(
                    R.string.duration,
                    positiveSeconds.hours,
                    positiveSeconds.minutes
                )
            } else {
                osUtilsProvider.stringFromResource(
                    R.string.duration_minutes,
                    positiveSeconds.minutes
                )
            }
        }
    }

    override fun formatSeconds(seconds: Int): String {
        return formatTimeValue(seconds.toSeconds())
    }

    override fun formatSeconds(seconds: Long): String {
        return formatTimeValue(seconds.toSeconds())
    }
}
