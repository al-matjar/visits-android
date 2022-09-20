package com.hypertrack.android.interactors.app.state

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import java.time.LocalDate

class AppViewStateTest {
    companion object {
        fun tabsView(): TabsView {
            return TabsView(
                currentTripTab = mockk(),
                historyTab = mockk() {
                    every { historyScreenState } returns mockk {
                        every { selectedDay } returns LocalDate.now()
                    }
                },
                placesTab = mockk(),
                ordersTab = mockk(),
                summaryTab = mockk(),
                profileTab = mockk(),
            )
        }
    }
}
