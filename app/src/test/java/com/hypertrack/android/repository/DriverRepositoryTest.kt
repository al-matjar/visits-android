package com.hypertrack.android.repository

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DriverRepositoryTest {

    @Test
    fun `it should correctly set device name`() {
        val slot = mutableListOf<String>()
        val driverRepository = DriverRepository(
            accountRepositoryProvider = mockk(relaxed = true) {
                every { get() } returns mockk(relaxed = true)
            },
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

        //note capitalization
        driverRepository.setUserData(email = "email@mail.com", driverId = "driver@mail.com")
        assertEquals("Email", slot.removeAt(0))

        driverRepository.setUserData(phoneNumber = "phone", driverId = "driver@mail.com")
        assertEquals("phone", slot.removeAt(0))

        driverRepository.setUserData(email = "email@mail.com", phoneNumber = "Phone")
        assertEquals("Email", slot.removeAt(0))

        driverRepository.setUserData(driverId = "driver@mail.com")
        assertEquals("Driver", slot.removeAt(0))

        driverRepository.setUserData(driverId = "driver Id")
        assertEquals("driver Id", slot.removeAt(0))
    }

    @Test
    fun `pick up device name from metadata`() {
        val slot = mutableListOf<String>()
        val driverRepository = DriverRepository(
            accountRepositoryProvider = mockk(relaxed = true) {
                every { get() } returns mockk(relaxed = true)
            },
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

        driverRepository.setUserData(
            email = "email@mail.com",
            metadata = mapOf(
                "name" to "metadata name",
            )
        )
        assertEquals("metadata name", slot.removeAt(0))
    }

}
