package com.hypertrack.android.use_case.handle_push

import android.app.NotificationManager
import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.TestInjector
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.userLoggedIn
import com.hypertrack.android.models.Integration
import com.hypertrack.android.use_case.handle_push.HandlePushUseCase.Companion.TRIP_NOTIFICATION_ID
import com.hypertrack.android.utils.FirebaseCrashReportsProviderTest.Companion.crashReportsProvider
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.OsUtilsProviderTest.Companion.resourceProvider
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.formatters.DateTimeFormatterImplTest.Companion.testDatetimeFormatter
import com.hypertrack.logistics.android.github.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.ZonedDateTime

class HandlePushUseCaseTest {

    @Test
    fun `handle trip notification`() {
        runBlocking {
            val mockTripsInteractor = mockk<TripsInteractor>() {
                coEvery { refreshTrips() } returns Unit
            }
            val notificationUtil = notificationUtil()

            handlePushUseCase(
                notificationUtil = notificationUtil
            ).execute(
                userLoggedIn(
                    userScope = mockk {
                        every { tripsInteractor } returns mockTripsInteractor
                    }
                ),
                remoteMessage(
                    type = "trip_update",
                    pushData = null
                )
            ).collect()

            coVerify(exactly = 1) { mockTripsInteractor.refreshTrips() }
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
                ).execute(
                    mockk(),
                    remoteMessage(
                        type = "outage",
                        pushData = notificationData
                    )
                ).collect()

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

    @Test
    fun `handle geofence visit notification`() {
        fun test(
            enter: ZonedDateTime,
            exit: ZonedDateTime? = null,
            name: String? = null,
            integration: Integration? = null,
            expectedText: String? = null,
            expectedTitle: String? = null,
        ) {
            runBlocking {
                val notificationUtil = notificationUtil()

                val notificationData = geofenceVisitNotificationData(
                    enter = enter,
                    exit = exit,
                    name = name,
                    integration = integration
                )
                handlePushUseCase(
                    notificationUtil = notificationUtil
                ).execute(
                    mockk(),
                    remoteMessage(
                        type = "geofence_visit",
                        pushData = notificationData
                    )
                ).collect()

                verify {
                    notificationUtil.sendNotification(
                        context = any(),
                        id = any(),
                        text = expectedText ?: any(),
                        title = expectedTitle ?: any(),
                        data = GeofenceVisitNotification(
                            geofenceId = "id",
                            geofenceName = name ?: integration?.name,
                            geofenceVisitTime = if (exit == null) {
                                EnterTime(enter)
                            } else {
                                ExitTime(enter, exit)
                            }
                        ),
                        type = GeofenceVisitNotification::class.java.simpleName,
                        autoCancel = true
                    )
                }
            }
        }

        test(
            enter = TEST_DATETIME,
            exit = null,
            expectedTitle = R.string.notification_geofence_enter.toString(),
            expectedText = resourceProvider().stringFromResource(
                R.string.notification_geofence_visit_text_without_name,
                testDatetimeFormatter().formatTime(TEST_DATETIME)
            )
        )

        test(
            enter = TEST_DATETIME,
            exit = TEST_DATETIME.plusDays(1),
            expectedTitle = R.string.notification_geofence_exit.toString(),
            expectedText = resourceProvider().stringFromResource(
                R.string.notification_geofence_visit_text_without_name,
                testDatetimeFormatter().formatTime(TEST_DATETIME.plusDays(1))
            )
        )
    }

    companion object {
        private val TEST_DATETIME = ZonedDateTime.now()

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

        fun geofenceVisitNotificationData(
            enter: ZonedDateTime,
            exit: ZonedDateTime? = null,
            name: String? = null,
            integration: Integration? = null,
        ): Map<String, Any> {
            return mutableMapOf(
                "arrival" to mapOf(
                    "recorded_at" to enter
                        .format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                ),
                "geofence_id" to "id",
                "geofence_metadata" to mapOf(
                    "name" to name,
                    "integration" to integration
                )
            ).apply {
                exit?.let {
                    put(
                        "exit", mapOf(
                            "recorded_at" to it.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                        )
                    )
                }
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

        private fun remoteMessage(type: String, pushData: Map<String, Any>?): RemoteMessage {
            return mockk {
                every { data } returns mapOf(
                    "lab_notification" to mutableMapOf<String, Any>(
                        "type" to type
                    ).also { labNotification ->
                        pushData?.let { labNotification["data"] = it }
                    }.let {
                        TestInjector.getMoshi().createAnyMapAdapter().toJson(it)
                    }
                )
            }
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
                moshi = TestInjector.getMoshi(),
                crashReportsProvider = crashReportsProvider(),
                notificationUtil = notificationUtil,
                resourceProvider = resourceProvider(),
                dateTimeFormatter = testDatetimeFormatter(),
            )
        }
    }

}
