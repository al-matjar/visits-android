package com.hypertrack.android.utils

open class DistanceUnit
class Meters(val meters: Int)

fun Int.toMeters() = Meters(this)