package com.hypertrack.android.models

sealed class MeasurementUnits
object Unspecified : MeasurementUnits()
object Imperial : MeasurementUnits()
object Metric : MeasurementUnits()
