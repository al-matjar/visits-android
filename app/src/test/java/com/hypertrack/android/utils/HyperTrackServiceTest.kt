package com.hypertrack.android.utils

import android.location.Location
import android.os.Build
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.screens.visits_management.tabs.places.Visit
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import com.hypertrack.sdk.HyperTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class HyperTrackServiceTest {

    @Test
    fun `it should set device info`() {
        val sdk = mockk<HyperTrack>(relaxed = true)
        val listener = TrackingState()

        val map = mapOf("1" to 1, "2" to mapOf("a" to "b"))

        val hyperTrackService = HyperTrackService(
            sdk = sdk,
            crashReportsProvider = crashReportsProvider()
        )
        hyperTrackService.setDeviceInfo(
            name = "name",
            email = "email",
            phoneNumber = "phoneNumber",
            driverId = "driverId",
            deeplinkWithoutGetParams = "deeplinkWithoutGetParams",
            metadata = map
        )

        val slot = slot<Map<String, Any>>()
        verify {
            sdk.setDeviceName("name")
            sdk.setDeviceMetadata(capture(slot))
        }
        assertEquals(map.toMutableMap().apply {
            put("email", "email")
            put("phone_number", "phoneNumber")
            put("driver_id", "driverId")
            put("invite_id", "deeplinkWithoutGetParams")
        }, slot.captured)
    }

    @Test
    fun `metadata fields priority`() {
        val sdk = mockk<HyperTrack>(relaxed = true)
        val listener = TrackingState()

        val map = mapOf(
            "driver_id" to "metadata driver id",
            "email" to "metadata email",
            "phone_number" to "metadata phone",
        )

        val hyperTrackService = HyperTrackService(
            sdk = sdk,
            crashReportsProvider = crashReportsProvider()
        )
        hyperTrackService.setDeviceInfo(
            name = "name",
            email = "email",
            phoneNumber = "phoneNumber",
            driverId = "driverId",
            deeplinkWithoutGetParams = "deeplinkWithoutGetParams",
            metadata = map
        )

        val slot = slot<Map<String, Any>>()
        verify {
            sdk.setDeviceMetadata(capture(slot))
        }
        assertEquals(map.toMutableMap().apply {
//            put("email", "email")
//            put("phone_number", "phoneNumber")
//            put("driver_id", "driverId")
            put("email", "metadata email")
            put("phone_number", "metadata phone")
            put("driver_id", "metadata driver id")
            put("invite_id", "deeplinkWithoutGetParams")
        }, slot.captured)
    }


}

