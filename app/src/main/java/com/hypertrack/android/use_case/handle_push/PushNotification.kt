package com.hypertrack.android.use_case.handle_push

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

sealed class PushNotification {
    val type: String = javaClass.simpleName
}

@Parcelize
data class GeofenceVisitNotification(
    val geofenceId: String,
    val geofenceVisitTime: GeofenceVisitTime,
    val geofenceName: String?,
) : PushNotification(), Parcelable

@Parcelize
data class OutageNotification(
    val outageCode: String,
    val outageType: String,
    val outageDisplayName: String,
    val outageDeveloperDescription: String,
    val userActionRequired: String
) : PushNotification(), Parcelable

object SdkNotification : PushNotification()
object TripUpdateNotification : PushNotification()
