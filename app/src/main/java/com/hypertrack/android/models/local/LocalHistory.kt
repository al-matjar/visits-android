package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.datetime.TimeValue
import com.hypertrack.android.utils.toMeters
import com.hypertrack.android.utils.datetime.toSeconds
import com.squareup.moshi.Moshi
import java.time.LocalDate

class LocalHistory(
    val date: LocalDate,
    val visits: List<LocalGeofenceVisit>,
    val geotags: List<Geotag>,
    val locations: List<LatLng>,
    val deviceStatusMarkers: List<DeviceStatusMarker>,
    val totalDriveDistance: DistanceValue,
    val totalDriveDuration: TimeValue,
)
