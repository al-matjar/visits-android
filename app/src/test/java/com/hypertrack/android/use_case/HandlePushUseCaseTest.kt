package com.hypertrack.android.use_case

import android.app.NotificationManager
import android.content.Intent
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.use_case.HandlePushUseCase.Companion.TRIP_NOTIFICATION_ID
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.createAnyMapAdapter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HandlePushUseCaseTest {

    @Test
    fun `handle trip notification`() {
        runBlocking {
            val tripsInteractor = mockk<TripsInteractor>() {
                coEvery { refreshTrips() } returns Unit
            }
            val notificationUtil = notificationUtil()

            handlePushUseCase(
                tripsInteractor = tripsInteractor,
                notificationUtil = notificationUtil
            ).execute(remoteMessage = mockk() {
                every { data } returns mapOf(
                    "visits" to "refresh"
                )
            }).collect()

            coVerify(exactly = 1) { tripsInteractor.refreshTrips() }
            verify {
                notificationUtil.sendNotification(
                    any(),
                    TRIP_NOTIFICATION_ID,
                    text = "text",
                    title = null,
                    intentAction = Intent.ACTION_SYNC,
                    type = TripUpdateNotification::class.java.simpleName,
                    autoCancel = true
                )
            }
        }
    }

    @Test
    fun `handle outage notification`() {
        runBlocking {
            val notificationUtil = notificationUtil()
            val notificationData = OutageNotification(
                outageDisplayName = "GPS signal lost",
                outageDescription = "description",
                outageCode = "code",
                userActionRequired = "action",
                outageType = "service_lost_by_os"
            )

            handlePushUseCase(
                notificationUtil = notificationUtil
            ).execute(remoteMessage = mockk() {
                every { data } returns mapOf(
                    "type" to "outage",
                    "data" to Injector.getMoshi().createAnyMapAdapter().toJson(
                        mapOf(
                            "inactive_reason" to mapOf(
                                "name" to "GPS signal lost",
                                "description" to "description",
                                "code" to "code",
                                "user_action_required" to "action",
                                "type" to "service_lost_by_os"
                            )
                        )
                    )
                )
            }).collect()

            verify {
                notificationUtil.sendNotification(
                    any(),
                    any(),
                    title = "GPS signal lost",
                    text = "description",
                    intentAction = null,
                    data = eq(notificationData),
                    type = OutageNotification::class.java.simpleName,
                    autoCancel = false
                )
            }
        }
    }

    companion object {
        fun notificationUtil(): NotificationUtil {
            return mockk {
                every {
                    sendNotification(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns JustSuccess
            }
        }

        fun handlePushUseCase(
            notificationUtil: NotificationUtil = notificationUtil(),
            tripsInteractor: TripsInteractor = mockk()
        ): HandlePushUseCase {
            return HandlePushUseCase(
                context = mockk() {
                    every { getSystemService(any()) } returns mockk<NotificationManager>() {
                        every {
                            notify(
                                any(),
                                any(),
                            )
                        } returns Unit
                    }
                },
                isLoggedInCheck = { true },
                moshi = Injector.getMoshi(),
                crashReportsProvider = mockk() {
                    every { logException(any()) } answers {
                        throw firstArg<Exception>()
                    }
                },
                notificationUtil = notificationUtil,
                resourceProvider = mockk() {
                    every { stringFromResource(any()) } returns "text"
                },
                tripsInteractorProvider = {
                    tripsInteractor
                }
            )
        }
    }

}
