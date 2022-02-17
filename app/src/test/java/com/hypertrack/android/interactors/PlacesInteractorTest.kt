package com.hypertrack.android.interactors

import com.hypertrack.android.TestInjector
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.mock.TestMockData
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.repository.PlacesRepositoryImpl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test
import retrofit2.Response

@ExperimentalCoroutinesApi
class PlacesInteractorTest {

    @Test
    fun `it should create a geofence with null address if address is blank`() {
        var slot: GeofenceMetadata = GeofenceMetadata(address = "default")
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { createGeofence(any(), any(), any(), any()) } coAnswers {
                slot = arg(3)
                Response.success(listOf(TestMockData.createGeofence()))
            }
        }
        val placesInteractor = PlacesInteractorImpl(
            PlacesRepositoryImpl(
                DeviceId("1"),
                apiClient,
                TestInjector.getMoshi(),
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            TestCoroutineScope(),
        )
        runBlocking {
            placesInteractor.createGeofence(
                1.1, 1.1, null,
                " ",
                100, null, null
            )
        }
        coVerify {
            apiClient.createGeofence(any(), any(), any(), any())
        }
        assertEquals(null, slot.address)
    }

}
