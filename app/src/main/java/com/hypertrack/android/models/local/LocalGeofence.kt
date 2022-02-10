package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.Polygon
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class LocalGeofence(
    val geofence: Geofence,
    val name: String?,
    val address: String?,
    val radius: Int,
    val latLng: LatLng,
    val isPolygon: Boolean,
    val polygon: List<LatLng>?,
    val integration: Integration?,
    val metadata: Map<String, String>,
    val visits: List<LocalGeofenceVisit>
) {

    val id = geofence.geofence_id

    val location: LatLng
        get() = geofence.geometry.let { LatLng(it.latitude, it.longitude) }

    val visitsCount: Int by lazy {
        visits.count()
    }

    val lastVisit: LocalGeofenceVisit? by lazy {
        visits.firstOrNull()
    }

    val createdAt: ZonedDateTime = dateTimeFromString(geofence.created_at)

    companion object {
        fun fromGeofence(
            currentDeviceId: DeviceId,
            geofence: Geofence,
            moshi: Moshi,
            osUtilsProvider: OsUtilsProvider,
            crashReportsProvider: CrashReportsProvider
        ): LocalGeofence {
            //all parsed metadata fields should be removed to avoid duplication
            val metadata = geofence.metadata?.toMutableMap() ?: mutableMapOf()

            val metadataAddress = metadata.remove(GeofenceMetadata.KEY_ADDRESS) as String?
            val address = geofence.address.nullIfBlank()
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

            val latLng = LatLng(geofence.latitude, geofence.longitude)
            val isPolygon = geofence.geometry is Polygon
            val polygon: List<LatLng>? = if (geofence.geometry is Polygon) {
                geofence.geometry.coordinates.first().map {
                    LatLng(it[1], it[0])
                }
            } else null


            return LocalGeofence(
                geofence = geofence,
                name = metadata.remove(GeofenceMetadata.KEY_NAME) as String?,
                address = address,
                latLng = latLng,
                isPolygon = isPolygon,
                polygon = polygon,
                radius = if (!isPolygon) {
                    geofence.radius!!
                } else {
                    calcRadius(latLng, polygon!!, osUtilsProvider)
                },
                integration = integration,
                metadata = metadata.filter { it.value is String } as Map<String, String>,
                visits = (geofence.marker?.visits
                    ?.filter { it.deviceId == currentDeviceId.value }
                    ?.sortedByDescending { it.arrival!!.recordedAt }
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
}
