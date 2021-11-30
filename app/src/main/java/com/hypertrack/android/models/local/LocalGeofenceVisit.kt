package com.hypertrack.android.models.local

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.ui.common.delegates.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.DeviceId
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetimeFromString
import com.squareup.moshi.Moshi
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class LocalGeofenceVisit(
    val id: String,
    val geofenceId: String,
    val deviceId: String,
    val arrival: ZonedDateTime,
    val exit: ZonedDateTime?,
    val location: LatLng,
    val routeTo: RouteTo?,
    val durationSeconds: Int?,
    val address: String?,
    val metadata: GeofenceMetadata?
) {
    fun getDay(): LocalDate {
        return if (exit != null) {
            exit.toLocalDate()
        } else {
            arrival.toLocalDate()
        }
    }

    companion object {
        fun fromVisit(visit: GeofenceVisit): LocalGeofenceVisit {
            return LocalGeofenceVisit(
                id = visit.markerId!!,
                geofenceId = visit.geofenceId,
                deviceId = visit.deviceId,
                arrival = datetimeFromString(visit.arrival!!.recordedAt),
                exit = visit.exit?.recordedAt?.let { datetimeFromString(it) },
                location = visit.geometry!!.let { LatLng(it.latitude, it.longitude) },
                routeTo = visit.routeTo?.let {
                    //todo workaround
                    if (it.idleTime == null && it.distance == null && it.duration == null) {
                        null
                    } else it
                },
                durationSeconds = visit.duration,
                address = visit.address,
                metadata = visit.metadata,
            )
        }

        fun fromGraphQlVisit(
            visit: GraphQlGeofenceVisit,
            deviceId: DeviceId,
            osUtilsProvider: OsUtilsProvider,
            crashReportsProvider: CrashReportsProvider,
            addressDelegate: GraphQlGeofenceVisitAddressDelegate,
            moshi: Moshi
        ): LocalGeofenceVisit {
            val exit = visit.exit?.recordedAt?.let { datetimeFromString(it) }
            val arrival = datetimeFromString(visit.arrival.recordedAt)
            val location = visit.geofence.location

            return LocalGeofenceVisit(
                id = visit.id,
                geofenceId = visit.geofence.geofence_id,
                deviceId = deviceId.value,
                arrival = arrival,
                exit = exit,
                location = location,
                routeTo = visit.routeTo?.let {
                    //todo workaround
                    if (it.idleTime == null && it.distance == null && it.duration == null) {
                        null
                    } else it
                },
                durationSeconds = exit?.let { ChronoUnit.SECONDS.between(arrival, exit) }?.toInt(),
                address = addressDelegate.displayAddress(visit),
                metadata = try {
                    moshi.adapter(GeofenceMetadata::class.java).fromJson(visit.geofence.metadata)
                } catch (e: Exception) {
                    crashReportsProvider.logException(e)
                    GeofenceMetadata.fromMap(mapOf(), crashReportsProvider)
                },
            )
        }
    }
}