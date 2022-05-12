package com.hypertrack.android.use_case.handle_push

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

sealed class GeofenceVisitTime : Parcelable

@Parcelize
data class EnterTime(
    val enterDateTime: ZonedDateTime,
) : GeofenceVisitTime()

@Parcelize
data class ExitTime(
    val enterDateTime: ZonedDateTime,
    val exitDateTime: ZonedDateTime,
) : GeofenceVisitTime()
