package com.hypertrack.android.interactors

import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlDayVisitsStats
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.utils.*
import com.squareup.moshi.Moshi
import java.time.LocalDate
import java.time.ZoneId

class PlacesVisitsRepository(
    private val deviceId: DeviceId,
    private val graphQlApiClient: GraphQlApiClient,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    //todo check concurrency issues
    private val cache = mutableMapOf<LocalDate, GraphQlDayVisitsStats>()

    @Suppress("ReplacePutWithAssignment")
    suspend fun getPlaceVisitsStats(): Result<PlaceVisitsStats> {
        return getAtLeastLastNMonthDates()
            .filter {
                !shouldCacheDay(it) || !cache.contains(it)
            }.let { daysToLoad ->
                graphQlApiClient.getPlaceVisitsStats(daysToLoad.map { DayRange(it, zoneId) })
            }.let { result ->
                when (result) {
                    is Success -> {
                        val loadedDays = result.data
                        handleSuccess(loadedDays)
                    }
                    is Failure -> Failure(result.exception)
                }
            }
    }

    private fun handleSuccess(loadedDays: Map<DayRange, GraphQlDayVisitsStats>): Success<PlaceVisitsStats> {
        return mutableMapOf<LocalDate, GraphQlDayVisitsStats>()
            .also { res ->
                loadedDays.forEach { (k, v) ->
                    cache.put(k.localDate, v)
                    res.put(k.localDate, v)
                }
                cache.forEach { (k, v) ->
                    res.put(k, v)
                }
            }
            .filter { !it.value.isEmpty() }
            .mapValues { (_, v) ->
                LocalDayVisitsStats(
                    v.visits.map {
                        LocalGeofenceVisit.fromGraphQlVisit(
                            it,
                            deviceId,
                            crashReportsProvider,
                            GraphQlGeofenceVisitAddressDelegate(osUtilsProvider),
                            moshi
                        )
                    },
                    v.totalDriveDistanceMeters.toMeters()
                )
            }
            .let { Success(PlaceVisitsStats(it)) }
    }

    fun invalidateCache() {
        cache.clear()
    }

    private fun shouldCacheDay(localDate: LocalDate): Boolean {
        val today = LocalDate.now()
        return !(localDate == today || localDate == today.minusDays(1))
    }

    private fun getAtLeastLastNMonthDates(
        baseDate: LocalDate = LocalDate.now(),
        monthNumber: Int = 2
    ): List<LocalDate> {
        return mutableListOf<LocalDate>().apply {
            for (i in 0 until 31 * monthNumber) {
                add(baseDate.minusDays(i.toLong()))
            }
        }
    }
}

class PlaceVisitsStats(
    stats: Map<LocalDate, LocalDayVisitsStats>
) : EnforceSortedKeyMap<LocalDate, LocalDayVisitsStats, ReverseDateComparator>(
    stats,
    ReverseDateComparator
)

data class LocalDayVisitsStats(
    val visits: List<LocalGeofenceVisit>,
    val totalDriveDistance: Meters
) {
    fun isEmpty(): Boolean {
        return visits.isEmpty() && totalDriveDistance.meters == 0
    }
}



