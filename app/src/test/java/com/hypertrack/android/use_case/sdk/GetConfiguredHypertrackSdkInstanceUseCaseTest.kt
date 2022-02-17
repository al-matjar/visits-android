package com.hypertrack.android.use_case.sdk

import com.hypertrack.android.TestInjector
import com.hypertrack.android.utils.toFlow
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.TrackingError
import com.hypertrack.sdk.TrackingStateObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GetConfiguredHypertrackSdkInstanceUseCaseTest {

    @Test
    fun `it should create hypertrack sdk instance with correct parameters`() {
        runBlocking {
            val listenerSlot = slot<TrackingStateObserver.OnTrackingStateChangeListener>()
            val listenerResults = mutableListOf<NewTrackingState>()
            val trackingStateListener = { state: NewTrackingState ->
                listenerResults.add(state)
                Unit
            }
            val mockSdk = mockk<HyperTrack> {
                every { addTrackingListener(capture(listenerSlot)) } returns this
                every { backgroundTrackingRequirement(false) } returns this
                every { setTrackingNotificationConfig(any()) } returns this
            }

            getConfiguredHypertrackSdkInstanceUseCase(
                sdk = mockSdk,
                trackingStateListener = trackingStateListener
            )
                .execute(TestInjector.TEST_PUBLISHABLE_KEY)
                .collect {
                    verify { mockSdk.setTrackingNotificationConfig(any()) }
                    verify { mockSdk.backgroundTrackingRequirement(false) }
                    listenerSlot.captured.onTrackingStart()
                    listenerSlot.captured.onTrackingStop()
                    listenerSlot.captured.onError(TrackingError())
                    assertEquals(
                        listOf(
                            TrackingStarted,
                            TrackingStopped,
                            TrackingFailure(code = 0, message = "", codeName = "UNKNOWN_ERROR")
                        ), listenerResults
                    )
                }
        }
    }

    companion object {
        fun getConfiguredHypertrackSdkInstanceUseCase(
            sdk: HyperTrack,
            trackingStateListener: (NewTrackingState) -> Unit
        ): GetConfiguredHypertrackSdkInstanceUseCase {
            return GetConfiguredHypertrackSdkInstanceUseCase(
                getHypertrackSdkInstanceUseCase = mockk {
                    every { execute(any()) } returns sdk.toFlow()
                },
                trackingStateListener = trackingStateListener
            )
        }
    }

}
