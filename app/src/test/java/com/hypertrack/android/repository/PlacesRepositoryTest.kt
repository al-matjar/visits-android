package com.hypertrack.android.repository

import com.hypertrack.android.TestMockData
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.GeofenceSuccess
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MockData
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PlacesRepositoryTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    //todo test all metadata
    @Test
    fun `it should correctly parse geofence integration in local geofence`() {
        val placesRepository = PlacesRepositoryImpl(
            "d1",
            mockk() {
                coEvery { getGeofence("1") } returns MockData.createGeofence(
                    polygon = true, metadata = mapOf(
                        "integration" to mapOf(
                            "name" to "ABC",
                            "id" to "1"
                        )
                    )
                )
            },
            Injector.getMoshi(),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

        runBlocking {
            (placesRepository.getGeofence("1") as GeofenceSuccess).geofence.apply {
                assertEquals("ABC", integration?.name)
                assertEquals("1", integration?.id)
            }
        }
    }

    @Test
    fun `it should filter visit from current device id`() {
        val placesRepository = PlacesRepositoryImpl(
            "device_id",
            mockk(relaxed = true) {
                coEvery { getGeofences(any(), any()) } returns GeofenceResponse(
                    listOf(
                        MockData.createGeofence().copy(
                            marker = GeofenceMarkersResponse(
                                listOf(
                                    MockData.createGeofenceVisit(deviceId = "other_device_id"),
                                    MockData.createGeofenceVisit(deviceId = "device_id")
                                ), null
                            )
                        )
                    ), null
                )

                coEvery { getGeofence(any()) } returns MockData.createGeofence().copy(
                    marker = GeofenceMarkersResponse(
                        listOf(
                            MockData.createGeofenceVisit(deviceId = "other_device_id"),
                            MockData.createGeofenceVisit(deviceId = "device_id")
                        ), null
                    )
                )
            },
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

        runBlocking {
            placesRepository.loadGeofencesPage(null).let {
                assertEquals(listOf("device_id"), it.items[0].visits.map { it.deviceId })
            }

            (placesRepository.getGeofence("id") as GeofenceSuccess).let {
                assertEquals(listOf("device_id"), it.geofence.visits.map { it.deviceId })
            }
        }

    }

}