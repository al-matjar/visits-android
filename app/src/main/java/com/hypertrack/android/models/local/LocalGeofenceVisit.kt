package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.datetime.EndDateTime
import com.hypertrack.android.utils.datetime.StartDateTime
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.Moshi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LocalGeofenceVisit(
    val id: String?,
    val geofenceId: String,
    val deviceId: String,
    val arrival: StartDateTime,
    val exit: EndDateTime?,
    val location: LatLng?,
    val routeTo: RouteTo?,
    val durationSeconds: Int?,
    val address: String?,
    val metadata: GeofenceMetadata?
) {
    fun getDay(): LocalDate {
        return if (exit != null) {
            exit.value.toLocalDate()
        } else {
            arrival.value.toLocalDate()
        }
    }

    companion object {
        fun fromVisit(visit: GeofenceVisit): LocalGeofenceVisit {
            return LocalGeofenceVisit(
                id = visit.markerId,
                geofenceId = visit.geofenceId,
                deviceId = visit.deviceId,
                arrival = StartDateTime(dateTimeFromString(visit.arrival.recordedAt)),
                exit = visit.exit?.recordedAt?.let { dateTimeFromString(it) }
                    ?.let { EndDateTime((it)) },
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
            crashReportsProvider: CrashReportsProvider,
            address: String?,
            moshi: Moshi
        ): LocalGeofenceVisit {
            val exit = visit.exit?.recordedAt?.let { dateTimeFromString(it) }
            val arrival = dateTimeFromString(visit.arrival.recordedAt)
            val location = visit.geofence.location

            return LocalGeofenceVisit(
                id = visit.id,
                geofenceId = visit.geofence.geofenceId,
                deviceId = deviceId.value,
                arrival = StartDateTime(arrival),
                exit = exit?.let { EndDateTime(it) },
                location = location,
                routeTo = visit.routeTo?.let {
                    //todo workaround
                    if (it.idleTime == null && it.distance == null && it.duration == null) {
                        null
                    } else it
                },
                durationSeconds = exit?.let { ChronoUnit.SECONDS.between(arrival, exit) }?.toInt(),
                address = address,
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
