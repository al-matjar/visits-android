package com.hypertrack.android.utils.datetime

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TimeValueTest {
    @Test
    fun `it should correctly get TimeValue structure`() {
        test(1, 0, 0)
        val hoursOptions = listOf(0, 1, 2)
        val minutesOptions = listOf(0, 1, 2)
        val secondsOptions = listOf(0, 1, 2)
        hoursOptions.forEach { hours ->
            minutesOptions.forEach { minutes ->
                secondsOptions.forEach { seconds ->
                    test(hours, minutes, seconds)
                }
            }
        }
    }

    private fun test(hours: Int, minutes: Int, seconds: Int) {
        println("$hours h, $minutes m, $seconds s")
        TimeValue(
            (hours * TimeValue.MINUTES_IN_HOUR * TimeValue.SECONDS_IN_MINUTE
                    + minutes * TimeValue.SECONDS_IN_MINUTE + seconds).toLong()
        ).let {
            assertEquals(hours, it.hours)
            assertEquals(minutes, it.minutes)
            assertEquals(seconds, it.seconds)
        }
    }

}
