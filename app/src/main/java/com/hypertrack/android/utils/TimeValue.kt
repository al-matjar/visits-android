package com.hypertrack.android.utils

import com.hypertrack.android.utils.TimeValue.Companion.MINUTES_IN_HOUR
import com.hypertrack.android.utils.TimeValue.Companion.SECONDS_IN_MINUTE
import kotlin.math.abs

interface TimeValue {
    val seconds: Long
    val minutes: Int
    val hours: Int

    companion object {
        const val MINUTES_IN_HOUR = 60
        const val SECONDS_IN_MINUTE = 60
    }
}

open class Seconds(final override val seconds: Long) : TimeValue {
    final override val minutes: Int = (seconds / SECONDS_IN_MINUTE).toInt()
    final override val hours: Int = (minutes / MINUTES_IN_HOUR)
}

class PositiveSeconds(seconds: Long) : Seconds(abs(seconds)) {
    constructor(timeValue: TimeValue) : this(timeValue.seconds)
}


fun Long.toSeconds() = Seconds(this)
fun Int.toSeconds() = Seconds(this.toLong())

