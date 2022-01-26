package com.hypertrack.android.api.graphql

import com.hypertrack.android.api.graphql.models.DayQueryKey
import com.hypertrack.android.api.graphql.models.GraphQlDayVisitsStats
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.api.graphql.queries.HistoryQuery
import com.hypertrack.android.api.graphql.queries.PlacesVisitsQuery
import com.hypertrack.android.api.graphql.queries.QueryBody
import com.hypertrack.android.utils.*
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class GraphQlApiClient(
    private val api: GraphQlApi,
    private val publishableKey: PublishableKey,
    private val deviceId: DeviceId,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
) {

    suspend fun getPlaceVisitsStats(days: List<DayRange>): Result<Map<DayRange, GraphQlDayVisitsStats>> {
        return try {
            val daysMap = days.map { it.getQueryKey() to it }.toMap()

            api.getPlacesVisits(
                QueryBody(
                    PlacesVisitsQuery(deviceId, publishableKey, days)
                )
            ).let {
                handleGraphQlResponse(it).map { result ->
                    result.mapKeys { (dayQueryKey, _) -> daysMap.getValue(dayQueryKey) }
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
            Failure(e)
        }
    }

    suspend fun getHistoryForDay(day: DayRange): Result<GraphQlHistory> {
        return try {
            api.getHistory(QueryBody(HistoryQuery(deviceId, publishableKey, day))).let { response ->
                handleGraphQlResponse(response).map {
                    it.result
                }
            }
        } catch (e: Exception) {
            Failure(e)
        }.also {
            if (it is Failure) {
                crashReportsProvider.logException(it.exception)
            }
        }
    }

    private fun <T> handleGraphQlResponse(response: Response<GraphQlApi.GraphQlResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            response.body()?.let { body ->
                if (body.data != null) {
                    Success(body.data)
                } else {
                    Failure(Exception("GraphQlErrors:\n\n${body.errors?.joinToString(",\n\n")}"))
                }
            } ?: Failure(Exception("GraphQL response has null body"))
        } else {
            Failure(HttpException(response))
        }
    }

}

data class DayRange(
    val localDate: LocalDate,
    private val zoneId: ZoneId
) {
    val zonedDateTimeStart: ZonedDateTime = ZonedDateTime.of(localDate, LocalTime.MIN, zoneId)
    val zonedDateTimeEnd: ZonedDateTime = ZonedDateTime.of(localDate, LocalTime.MAX, zoneId)

    override fun toString(): String {
        return "[${localDate}, zoneId=$zoneId]"
    }

    fun getQueryKey(): DayQueryKey {
        return localDate.toString().replace("-", "_").let {
            "day${it}"
        }
    }
}
