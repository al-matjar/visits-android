package com.hypertrack.android.repository

import com.hypertrack.android.TestInjector
import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlDayVisitsStats
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.interactors.PlaceVisitsStats
import com.hypertrack.android.interactors.PlacesVisitsRepository
import com.hypertrack.android.mock.GeofenceVisitMockData
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.datetime.toIso
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PlacesVisitsRepositoryTest {

    @Test
    fun `it should load place visits and day stats and filter days with no data`() {
        val dt = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))

        val placesVisitsRepository = createPlacesVisitsRepository(
            mockk() {
                coEvery { getPlaceVisitsStats(any()) } returns Success(
                    mapOf(
                        dt.toDayRange() to GraphQlDayVisitsStats(
                            listOf(
                                createVisit(arrival = dt)
                            ),
                            100
                        ),
                        dt.plusDays(1).toDayRange() to GraphQlDayVisitsStats(
                            listOf(
                                createVisit(arrival = dt.plusDays(1))
                            ),
                            200
                        ),
                        dt.plusDays(2).toDayRange() to GraphQlDayVisitsStats(listOf(), 0)
                    )
                )
            }
        )


        runBlocking {
            placesVisitsRepository.getPlaceVisitsStats()
                .let { (it as Success<PlaceVisitsStats>).data }
                .let {
                    val stats = it.data
                    //it should filter empty items
                    TestCase.assertEquals(2, stats.size)
                    stats[dt.toLocalDate()]!!.let {
                        TestCase.assertEquals(
                            listOf(dt.toIso()),
                            it.visits.map { it.arrival.value.toIso() })
                        TestCase.assertEquals(
                            100,
                            it.totalDriveDistance.meters
                        )
                    }
                    stats[dt.plusDays(1).toLocalDate()]!!.let {
                        TestCase.assertEquals(
                            listOf(dt.plusDays(1).toIso()),
                            it.visits.map { it.arrival.value.toIso() })
                        TestCase.assertEquals(
                            200,
                            it.totalDriveDistance.meters
                        )
                    }
                }
        }
    }

    @Test
    fun `it should cache all days except current and the day before`() {
        val today = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
        val yesterday = ZonedDateTime.now().minusDays(1).withZoneSameInstant(ZoneId.of("UTC"))
        //should be in range of last 62 days
        val longTimeAgo = ZonedDateTime.now().minusDays(10).withZoneSameInstant(ZoneId.of("UTC"))

        val slot = mutableListOf<List<DayRange>>()

        val graphClient = mockk<GraphQlApiClient>() {
            coEvery { getPlaceVisitsStats(capture(slot)) } answers {
                val res = mapOf(
                    today.toLocalDate() to GraphQlDayVisitsStats(
                        listOf(
                            createVisit(arrival = today)
                        ),
                        0
                    ),
                    yesterday.toLocalDate() to GraphQlDayVisitsStats(
                        listOf(
                            createVisit(arrival = yesterday)
                        ),
                        0
                    ),
                    longTimeAgo.toLocalDate() to GraphQlDayVisitsStats(
                        listOf(
                            createVisit(arrival = longTimeAgo)
                        ),
                        0
                    )
                )

                Success(mutableMapOf<DayRange, GraphQlDayVisitsStats>().apply {
                    firstArg<List<DayRange>>().map { it.localDate }.forEach {
                        put(
                            it.toDayRange(),
                            if (res.containsKey(it)) {
                                res.getValue(it)
                            } else {
                                GraphQlDayVisitsStats(listOf(), 0)
                            }
                        )
                    }
                })
            }
        }

        val placesVisitsRepository = createPlacesVisitsRepository(graphClient)

        runBlocking {
            placesVisitsRepository.getPlaceVisitsStats()
            placesVisitsRepository.getPlaceVisitsStats()
            placesVisitsRepository.invalidateCache()
            placesVisitsRepository.getPlaceVisitsStats()

            val requestedDates = slot.map { it.map { it.localDate } }

            println(requestedDates[0])
            println(requestedDates[1])
            println(requestedDates[2])

            requestedDates[0].let {
                TestCase.assertTrue(it.contains(today.toLocalDate()))
                TestCase.assertTrue(it.contains(yesterday.toLocalDate()))
                TestCase.assertTrue(it.contains(longTimeAgo.toLocalDate()))
                TestCase.assertEquals(62, it.size)
            }

            requestedDates[1].let {
                TestCase.assertTrue(it.contains(today.toLocalDate()))
                TestCase.assertTrue(it.contains(yesterday.toLocalDate()))
                TestCase.assertTrue(!it.contains(longTimeAgo.toLocalDate()))
                TestCase.assertEquals(2, it.size)
            }

            requestedDates[2].let {
                TestCase.assertTrue(it.contains(today.toLocalDate()))
                TestCase.assertTrue(it.contains(yesterday.toLocalDate()))
                TestCase.assertTrue(it.contains(longTimeAgo.toLocalDate()))
                TestCase.assertEquals(62, it.size)
            }
        }
    }

    companion object {
        fun createPlacesVisitsRepository(graphQlApiClient: GraphQlApiClient): PlacesVisitsRepository {
            return PlacesVisitsRepository(
                DeviceId("device_id"),
                graphQlApiClient,
                mockk(relaxed = true),
                mockk(relaxed = true),
                TestInjector.getMoshi(),
                ZoneId.of("UTC")
            )
        }

        fun createVisit(
            arrival: ZonedDateTime = ZonedDateTime.now(),
            deviceId: String = "device_id"
        ): GraphQlGeofenceVisit {
            return GeofenceVisitMockData.createGraphQlGeofenceVisit(arrival = arrival)
        }
    }

}

fun ZonedDateTime.toDayRange() = DayRange(toLocalDate(), zone)

fun LocalDate.toDayRange() = DayRange(this, ZoneId.of("UTC"))
