package com.hypertrack.android.utils

import kotlin.math.roundToInt

interface DistanceValue {
    val meters: Int
}

class Meters(override val meters: Int) : DistanceValue
class Steps(val steps: Int) : DistanceValue {
    override val meters: Int = (steps * METERS_IN_STEP).roundToInt()

    companion object {
        const val METERS_IN_STEP = 0.76
    }
}

fun Int.toMeters() = Meters(this)
fun Int.toSteps() = Steps(this)
