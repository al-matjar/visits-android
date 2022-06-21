package com.hypertrack.android.use_case.history

import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.di.Injector.crashReportsProvider
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.DeviceStatusMarker
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.models.local.History
import com.hypertrack.android.models.local.Summary
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.datetime.toSeconds
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.android.utils.toMeters
import com.squareup.moshi.Moshi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.ZoneId

@Suppress("OPT_IN_USAGE")
class LoadHistoryUseCase(
    private val graphQlApiClient: GraphQlApiClient,
    private val visitAddressDelegate: GraphQlGeofenceVisitAddressDelegate,
    private val moshi: Moshi
) {

    fun execute(date: LocalDate, deviceId: DeviceId): Flow<Result<History>> {
        return suspend {
            val zoneId = ZoneId.systemDefault()
            graphQlApiClient.getHistoryForDay(
                DayRange(date, zoneId)
            )
        }.asFlow()
            .mapSuccess {
                mapRemoteHistory(
                    date,
                    it,
                    deviceId,
                    crashReportsProvider,
                    moshi
                )
            }
    }

    private suspend fun mapRemoteHistory(
        date: LocalDate,
        gqlHistory: GraphQlHistory,
        deviceId: DeviceId,
        crashReportsProvider: CrashReportsProvider,
        moshi: Moshi
    ): History {
        return History(
            date,
            getGeocodingAddresses(gqlHistory.visits).let { addresses ->
                gqlHistory.visits.map {
                    LocalGeofenceVisit.fromGraphQlVisit(
                        it,
                        deviceId,
                        crashReportsProvider,
                        addresses[it],
                        moshi
                    )
                }
            },
            gqlHistory.geotagMarkers.map {
                Geotag.fromGraphQlGeotagMarker(it, moshi)
            },
            gqlHistory.locations
                .sortedBy { it.recordedAt }
                .map { it.coordinate.toLatLng() },
            gqlHistory.deviceStatusMarkers.map {
                DeviceStatusMarker.fromGraphQl(it)
            },
            Summary(
//                for some reason there is no total distance in GraphQl API
//                totalDistance = gqlHistory.totalDistance.toMeters(),
//                for some reason there is no total duration in GraphQl API
//                totalDuration = gqlHistory.totalDuration.toSeconds(),
                stepsCount = gqlHistory.stepsCount,
                totalWalkDuration = gqlHistory.totalWalkDuration.toSeconds(),
                totalStopDuration = gqlHistory.totalStopDuration.toSeconds(),
                totalDriveDistance = gqlHistory.totalDriveDistanceMeters.toMeters(),
                totalDriveDuration = gqlHistory.totalDriveDurationMinutes.toSeconds()
            )
        )
    }

    private suspend fun getGeocodingAddresses(
        visits: List<GraphQlGeofenceVisit>
    ): Map<GraphQlGeofenceVisit, String?> {
        val addresses = mutableMapOf<GraphQlGeofenceVisit, String?>()
        try {
            withTimeout(5000L) {
                //todo parallelize
                visits.forEach { visit ->
                    addresses[visit] = visitAddressDelegate.displayAddress(visit)
                }
            }
        } catch (e: TimeoutCancellationException) {
        }
        return addresses
    }

}
