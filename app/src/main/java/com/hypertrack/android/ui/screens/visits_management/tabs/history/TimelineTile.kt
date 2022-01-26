package com.hypertrack.android.ui.screens.visits_management.tabs.history

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.models.local.OutageReason
import com.hypertrack.android.models.local.UserActivity
import java.time.ZonedDateTime

//todo locations
data class TimelineTile(
    val datetime: ZonedDateTime,
    val payload: TimelineTilePayload,
    val isStart: Boolean,
    val isOutage: Boolean,
    val description: String,
    val timeString: String?,
    val address: String?,
    val locations: List<LatLng>?
)

sealed class TimelineTilePayload {
    override fun toString(): String = javaClass.simpleName
}

class ActiveStatusTile(val activity: UserActivity) : TimelineTilePayload()
class InactiveStatusTile(val outageReason: OutageReason) : TimelineTilePayload()
class GeotagTile(val geotag: Geotag) : TimelineTilePayload()
class GeofenceVisitTile(val visit: LocalGeofenceVisit) : TimelineTilePayload()
