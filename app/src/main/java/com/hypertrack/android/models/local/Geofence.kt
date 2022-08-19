package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Polygon
import com.hypertrack.android.api.models.RemoteGeofence
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class Geofence(
    val id: GeofenceId,
    val name: String?,
    val address: String?,
    val radius: Int,
    val location: LatLng,
    val createdAt: ZonedDateTime,
    val isPolygon: Boolean,
    val polygon: List<LatLng>?,
    val integration: Integration?,
    val metadata: Map<String, String>,
    val visits: List<LocalGeofenceVisit>
) {

    val visitsCount: Int by lazy {
        visits.count()
    }

    val lastVisit: LocalGeofenceVisit? by lazy {
        visits.firstOrNull()
    }

    companion object {
        fun fromRemoteGeofence(
            remoteGeofence: RemoteGeofence,
            currentDeviceId: DeviceId,
            moshi: Moshi,
            osUtilsProvider: OsUtilsProvider,
            crashReportsProvider: CrashReportsProvider
        ): Geofence {
            //all parsed metadata fields should be removed to avoid duplication
            val metadata = remoteGeofence.metadata?.toMutableMap() ?: mutableMapOf()

            val metadataAddress = metadata.remove(GeofenceMetadata.KEY_ADDRESS) as String?
            val address = remoteGeofence.address.nullIfBlank()
                ?: metadataAddress.nullIfBlank()

            val integration = metadata.remove(GeofenceMetadata.KEY_INTEGRATION)?.let {
                try {
                    moshi.adapter(Integration::class.java)
                        .fromJsonValue(it)
                } catch (e: Exception) {
                    crashReportsProvider.logException(e)
                    null
                }
            }

            val latLng = LatLng(remoteGeofence.latitude, remoteGeofence.longitude)
            val isPolygon = remoteGeofence.geometry is Polygon
            val polygon: List<LatLng>? = if (remoteGeofence.geometry is Polygon) {
                remoteGeofence.geometry.coordinates.first().map {
                    LatLng(it[1], it[0])
                }
            } else null


            return Geofence(
                id = GeofenceId(remoteGeofence.id),
                name = metadata.remove(GeofenceMetadata.KEY_NAME) as String?,
                address = address,
                location = latLng,
                createdAt = dateTimeFromString(remoteGeofence.created_at),
                isPolygon = isPolygon,
                polygon = polygon,
                radius = if (!isPolygon) {
                    remoteGeofence.radius!!
                } else {
                    calcRadius(latLng, polygon!!, osUtilsProvider)
                },
                integration = integration,
                metadata = metadata.filter { it.value is String } as Map<String, String>,
                visits = (remoteGeofence.marker?.visits
                    ?.filter { it.deviceId == currentDeviceId.value }
                    ?.sortedByDescending { it.arrival.recordedAt }
                    ?: listOf())
                    .map {
                        LocalGeofenceVisit.fromVisit(it)
                    }

            )
        }

        private fun calcRadius(
            latLng: LatLng,
            polygon: List<LatLng>,
            osUtilsProvider: OsUtilsProvider
        ): Int {
            //todo merge with Intersect
            return polygon.map { osUtilsProvider.distanceMeters(latLng, it) }.maxOrNull()!!
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Geofence) {
            other.id == id
        } else false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
