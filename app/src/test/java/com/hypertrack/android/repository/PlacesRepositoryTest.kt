package com.hypertrack.android.repository

import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.GeofenceSuccess
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

                coEvery { getAllGeofencesVisits(any()) } returns VisitsResponse(
                    listOf(
                        MockData.createGeofenceVisit(deviceId = "other_device_id"),
                        MockData.createGeofenceVisit(deviceId = "device_id")
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
        )

        runBlocking {
            placesRepository.loadGeofencesPage(null).let {
                assertEquals(listOf("device_id"), it.geofences[0].markers.map { it.deviceId })
            }

            placesRepository.loadAllGeofencesVisitsPage(null).let {
                assertEquals(listOf("device_id"), it.items.map { it.deviceId })
            }

            (placesRepository.getGeofence("id") as GeofenceSuccess).let {
                assertEquals(listOf("device_id"), it.geofence.markers.map { it.deviceId })
            }
        }

    }

}