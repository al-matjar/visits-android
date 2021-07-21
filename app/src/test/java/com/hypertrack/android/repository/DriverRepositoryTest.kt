package com.hypertrack.android.repository

import com.hypertrack.android.ui.common.isEmail
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DriverRepositoryTest {

    @Test
    fun `it should correctly set device name`() {
        val slot = mutableListOf<String>()
        val driverRepository = DriverRepository(
            mockk(relaxed = true),
            mockk(relaxed = true) {
                every { getHyperTrackService(any()) } returns mockk(relaxed = true) {
                    every {
                        setDeviceInfo(
                            capture(slot),
                            any(),
                            any(),
                            any(),
                            any(),
                            any(),
                        )
                    } returns Unit
                }
            },
            mockk(relaxed = true),
            mockk(relaxed = true) {
                every { isEmail(any()) } answers { firstArg<String>().contains("@") }
            },
            mockk(relaxed = true),
        )

        driverRepository.setUserData(email = "email@mail.com", driverId = "driver@mail.com")
        assertEquals("email", slot.removeAt(0))

        driverRepository.setUserData(phoneNumber = "Phone", driverId = "driver@mail.com")
        assertEquals("Phone", slot.removeAt(0))

        driverRepository.setUserData(email = "email@mail.com", phoneNumber = "Phone")
        assertEquals("email", slot.removeAt(0))

        driverRepository.setUserData(driverId = "driver@mail.com")
        assertEquals("driver", slot.removeAt(0))

        driverRepository.setUserData(driverId = "Driver Id")
        assertEquals("Driver Id", slot.removeAt(0))
    }

}