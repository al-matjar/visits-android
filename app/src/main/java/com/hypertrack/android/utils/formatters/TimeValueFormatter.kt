package com.hypertrack.android.utils.formatters

import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.PositiveSeconds
import com.hypertrack.android.utils.datetime.TimeValue
import com.hypertrack.android.utils.datetime.toSeconds
import com.hypertrack.logistics.android.github.R

interface TimeValueFormatter {
    fun formatTimeValue(timeValue: TimeValue): String

    //todo remove legacy
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

}
