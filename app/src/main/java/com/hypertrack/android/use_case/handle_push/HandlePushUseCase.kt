package com.hypertrack.android.use_case.handle_push

import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.models.LiveAppBackendNotification
import com.hypertrack.android.models.OutageType
import com.hypertrack.android.models.webhook.GeofenceWebhook
import com.hypertrack.android.models.webhook.OutageWebhook
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.hypertrack.android.utils.flatMapSimpleSuccess
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlin.random.Random

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class HandlePushUseCase(
    private val context: Context,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
    private val resourceProvider: ResourceProvider,
    private val notificationUtil: NotificationUtil,
    private val dateTimeFormatter: DateTimeFormatter,
) {

    fun execute(userLoggedIn: UserLoggedIn, remoteMessage: RemoteMessage): Flow<Unit> {
        return suspend {
            crashReportsProvider.log("Got push notification: ${remoteMessage.data}")
            getRemoteMessageData(remoteMessage)
        }.asFlow().flatMapSuccess { data ->
            deserializeNotification(data)
        }.flatMapSimpleSuccess { notification ->
            handleNotification(userLoggedIn, notification)
        }.flatMapConcat {
            when (it) {
                is JustFailure -> {
                    if (MyApplication.DEBUG_MODE) {
                        NotificationUtil.sendNotification(
                            context,
                            DEBUG_NOTIFICATION_ID,
                            title = "Unknown notification",
                            text = remoteMessage.data.toString(),
                            type = "debug"
                        )
                    }
                    crashReportsProvider.logException(it.exception)
                }
                JustSuccess -> {
                }
            }.toFlow()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun getRemoteMessageData(remoteMessage: RemoteMessage): Result<Map<String, String>> {
        return tryAsResult {
            remoteMessage.data.ifEmpty {
                try {
                    // this is for debug only, you can send a test message from Firebase admin panel with text
                    // that contains JSON that mimics the format of real message payload
                    remoteMessage.notification?.body?.let { data ->
                        moshi.createAnyMapAdapter().fromJson(data)!!.mapValues {
                            when (it.value) {
                                is Map<*, *> -> moshi.createAnyMapAdapter()
                                    .toJson(it.value as Map<String, Any>)
                                else -> it.value.toString()
                            }
                        }
                    } ?: mapOf()
                } catch (e: Exception) {
                    mapOf()
                }
            }
        }
    }

    private fun deserializeNotification(data: Map<String, String>): Flow<Result<PushNotification>> {
        return tryAsResult {
            when {
                data[KEY_HYPERTRACK] != null -> {
                    SdkNotification.asSuccess()
                }
                data[KEY_PUSH] != null -> {
                    moshi.adapter(LiveAppBackendNotification::class.java)
                        .fromJson(data.getValue(KEY_PUSH))
                        ?.let { labNotification ->
                            val labPushData = labNotification.data ?: mapOf()
                            when (labNotification.type) {
                                TYPE_TRIP_UPDATE -> {
                                    TripUpdateNotification.asSuccess()
                                }
                                TYPE_OUTAGE -> {
                                    moshi.adapter(OutageWebhook::class.java)
                                        .fromJsonValue(labPushData)?.let {
                                            OutageNotification(
                                                outageCode = it.inactiveReason.outageCode,
                                                outageType = it.inactiveReason.outageType,
                                                outageDisplayName = it.inactiveReason.outageDisplayName,
                                                outageDeveloperDescription = it.inactiveReason
                                                    .outageDeveloperDescription,
                                                userActionRequired = it.inactiveReason.userActionRequired,
                                            ).asSuccess()
                                        } ?: Failure(NullPointerException("moshi: $data"))
                                }
                                TYPE_GEOFENCE_VISIT -> {
                                    moshi.adapter(GeofenceWebhook::class.java)
                                        .fromJsonValue(labPushData)?.let { webhook ->
                                            GeofenceVisitNotification(
                                                geofenceId = webhook.geofenceId,
                                                geofenceVisitTime = if (webhook.exit == null) {
                                                    EnterTime(dateTimeFromString(webhook.arrival.recordedAt))
                                                } else {
                                                    ExitTime(
                                                        enterDateTime = dateTimeFromString(webhook.arrival.recordedAt),
                                                        exitDateTime = dateTimeFromString(webhook.exit.recordedAt),
                                                    )
                                                },
                                                geofenceName = webhook.metadata?.name
                                                    ?: webhook.metadata?.integration?.name
                                            ).asSuccess()
                                        } ?: Failure(NullPointerException("moshi: $data"))
                                }
                                else -> {
                                    UnknownPushNotificationException(data).asFailure()
                                }
                            }
                        } ?: throw NullPointerException()
                }
                else -> {
                    UnknownPushNotificationException(data).asFailure()
                }
            }
        }.toFlow().flatMapSuccess { result ->
            result.map {
                // to avoid type casting issue
                it as PushNotification
            }.toFlow()
        }
    }

    private suspend fun handleNotification(
        state: UserLoggedIn,
        notification: PushNotification
    ): Flow<SimpleResult> {
        return when (notification) {
            TripUpdateNotification -> {
                state.userScope.tripsInteractor.refreshTrips()
                notificationUtil.sendNotification(
                    context,
                    id = TRIP_NOTIFICATION_ID,
                    text = resourceProvider.stringFromResource(
                        R.string.notification_new_trip
                    ),
                    intentAction = Intent.ACTION_SYNC,
                    type = notification.javaClass.simpleName
                )
            }
            is OutageNotification -> {
                notificationUtil.sendNotification(
                    context,
                    id = getRandomNotificationId(),
                    title = getNotificationTitle(notification),
                    text = notification.outageDeveloperDescription,
                    data = notification,
                    type = notification.javaClass.simpleName,
                )
            }
            is GeofenceVisitNotification -> {
                val enter = notification.geofenceVisitTime is EnterTime
                val timeString = when (val time = notification.geofenceVisitTime) {
                    is EnterTime -> dateTimeFormatter.formatTime(time.enterDateTime)
                    is ExitTime -> dateTimeFormatter.formatTime(time.exitDateTime)
                }
                val text = if (notification.geofenceName != null) {
                    resourceProvider.stringFromResource(
                        R.string.notification_geofence_visit_text,
                        notification.geofenceName,
                        timeString
                    )
                } else {
                    resourceProvider.stringFromResource(
                        R.string.notification_geofence_visit_text_without_name,
                        timeString
                    )
                }
                notificationUtil.sendNotification(
                    context,
                    id = getRandomNotificationId(),
                    title = resourceProvider.stringFromResource(
                        if (enter) {
                            R.string.notification_geofence_enter
                        } else {
                            R.string.notification_geofence_exit
                        }
                    ),
                    text = text,
                    data = notification,
                    type = notification.javaClass.simpleName,
                )
            }
            SdkNotification -> {
                // ignore SDK notifications
                JustSuccess
            }
        }.toFlow()
    }

    private fun getNotificationTitle(notification: OutageNotification): String {
        return when {
            OutageType.SERVICE_TERMINATED_GROUP.map { it.name }
                .contains(notification.outageType) -> {
                resourceProvider.stringFromResource(R.string.notification_service_was_terminated)
            }
            else -> notification.outageDisplayName
        }
    }

    private fun getRandomNotificationId(): Int {
        return Random.nextInt(1000, Int.MAX_VALUE)
    }

    companion object {
        const val DEBUG_NOTIFICATION_ID = 0
        const val TRIP_NOTIFICATION_ID = 1

        private const val KEY_PUSH = "lab_notification"
        private const val KEY_HYPERTRACK = "hypertrack"

        private const val TYPE_TRIP_UPDATE = "trip_update"
        private const val TYPE_OUTAGE = "outage"
        private const val TYPE_GEOFENCE_VISIT = "geofence_visit"
    }

}

