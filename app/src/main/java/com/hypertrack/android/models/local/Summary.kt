package com.hypertrack.android.models.local

import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.datetime.TimeValue

data class Summary(
//    for some reason there is no total distance in GraphQl API
//    val totalDistance: DistanceValue,
//    for some reason there is no total duration in GraphQl API
//    val totalDuration: TimeValue,
    val totalDriveDistance: DistanceValue,
    val totalDriveDuration: TimeValue,
    val stepsCount: Int,
    val totalWalkDuration: TimeValue,
    val totalStopDuration: TimeValue,
)
