package com.hypertrack.android.interactors.app.state

import io.mockk.mockk
import junit.framework.TestCase

class AppViewStateTest {
    companion object {
        fun tabsView(): TabsView {
            return TabsView(
                currentTripTab = mockk(),
                historyTab = mockk(),
                placesTab = mockk(),
                ordersTab = mockk(),
                summaryTab = mockk(),
                profileTab = mockk(),
            )
        }
    }
}
