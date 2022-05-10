package com.hypertrack.android.use_case

import android.app.NotificationManager
import android.content.Intent
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.use_case.HandlePushUseCase.Companion.TRIP_NOTIFICATION_ID
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.logistics.android.github.R
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
                    text = R.string.notification_new_trip.toString(),
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
        fun test(type: String, expectedTitle: String? = null) {
            runBlocking {
                val notificationUtil = notificationUtil()
                val notificationData = outageNotificationData(type)

                handlePushUseCase(
                    notificationUtil = notificationUtil
                ).execute(remoteMessage = mockk() {
                    every { data } returns mapOf(
                        "type" to "outage",
                        "data" to Injector.getMoshi().createAnyMapAdapter().toJson(
                            notificationData
                        )
                    )
                }).collect()

                val notification = outageNotification(notificationData)
                verify {
                    notificationUtil.sendNotification(
                        any(),
                        any(),
                        title = expectedTitle ?: notification.outageDisplayName,
                        text = notification.outageDeveloperDescription,
                        intentAction = null,
                        data = eq(notification),
                        type = OutageNotification::class.java.simpleName,
                        autoCancel = true
                    )
                }
            }
        }

        test(
            "service_terminated",
            expectedTitle = R.string.notification_service_was_terminated.toString()
        )
        test(
            "service_terminated_by_user",
            expectedTitle = R.string.notification_service_was_terminated.toString()
        )
        test(
            "service_terminated_by_os",
            expectedTitle = R.string.notification_service_was_terminated.toString()
        )
        test("service_lost_by_os")
    }

    companion object {
        fun outageNotification(map: Map<String, Any>): OutageNotification {
            return map.getValue("inactive_reason")
                .let { it as Map<String, String> }
                .let { inactiveReason ->
                    OutageNotification(
                        outageDisplayName = inactiveReason.getValue("name"),
                        outageDeveloperDescription = inactiveReason.getValue("description"),
                        outageCode = inactiveReason.getValue("code"),
                        userActionRequired = inactiveReason.getValue("user_action_required"),
                        outageType = inactiveReason.getValue("type")
                    )
                }
        }

        fun outageNotificationData(
            type: String? = null
        ): Map<String, Any> {
            return mapOf(
                "inactive_reason" to mapOf(
                    "name" to "GPS signal lost",
                    "description" to "description",
                    "code" to "code",
                    "user_action_required" to "action",
                    "type" to (type ?: "service_lost_by_os")
                )
            )
        }

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
                    every { stringFromResource(any()) } answers {
                        firstArg<Int>().toString()
                    }
                },
                tripsInteractorProvider = {
                    tripsInteractor
                }
            )
        }
    }

}
