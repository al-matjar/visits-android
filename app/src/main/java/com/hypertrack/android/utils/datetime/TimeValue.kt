package com.hypertrack.android.utils.datetime

import kotlin.math.abs

open class TimeValue(val totalSeconds: Long) {
    val seconds: Int = (totalSeconds % SECONDS_IN_MINUTE).toInt()
    val minutes: Int = ((totalSeconds - seconds) / SECONDS_IN_MINUTE)
        .let { totalMinutesLeft ->
            (totalMinutesLeft % MINUTES_IN_HOUR).toInt()
        }
    val hours: Int =
        (totalSeconds - minutes * SECONDS_IN_MINUTE - seconds).let { secondsLeft ->
            secondsLeft / (MINUTES_IN_HOUR * SECONDS_IN_MINUTE)
        }.toInt()

    companion object {
        const val MINUTES_IN_HOUR = 60
        const val SECONDS_IN_MINUTE = 60
    }
}

open class Seconds(value: Long) : TimeValue(value) {
    constructor(value: Int) : this(value.toLong())
}

class PositiveSeconds(seconds: Long) : Seconds(abs(seconds)) {
    constructor(timeValue: TimeValue) : this(timeValue.totalSeconds)
}

fun Long.toSeconds() = Seconds(this)
fun Int.toSeconds() = Seconds(this)
