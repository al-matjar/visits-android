package com.hypertrack.android.use_case

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.flatMapSimpleSuccess
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import java.lang.NullPointerException
import kotlin.random.Random

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class HandlePushUseCase(
    private val context: Context,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
    private val resourceProvider: ResourceProvider,
    private val notificationUtil: NotificationUtil,
    private val tripsInteractorProvider: () -> TripsInteractor?,
    private val isLoggedInCheck: () -> Boolean
) {

    fun execute(remoteMessage: RemoteMessage): Flow<Unit> {
        return suspend {
            getPushData(remoteMessage)
        }.asFlow().flatMapSuccess { data ->
            parseNotificationData(data)
        }.flatMapSimpleSuccess { notification ->
            handleNotification(notification)
        }.flatMapConcat {
            when (it) {
                is JustFailure -> {
                    crashReportsProvider.logException(it.exception)
                }
                JustSuccess -> {
                }
            }.toFlow()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun getPushData(remoteMessage: RemoteMessage): Result<Map<String, String>> {
        Log.v(javaClass.simpleName, remoteMessage.data.toString())
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

    private fun parseNotificationData(data: Map<String, String>): Flow<Result<PushNotification>> {
        return tryAsResult {
            when {
                data[KEY_HYPERTRACK] != null -> {
                    SdkNotification.asSuccess()
                }
                data[KEY_NOTIFICATION_TYPE] != null -> {
                    when (data[KEY_NOTIFICATION_TYPE]) {
                        TYPE_OUTAGE -> {
                            moshi.createAnyMapAdapter()
                                .fromJson(data.getValue(KEY_PUSH_DATA) as String)
                                ?.let {
                                    it.getValue(KEY_INACTIVE_REASON) as Map<String, String>
                                }?.let { inactiveReason ->
                                    OutageNotification(
                                        outageCode = inactiveReason.getValue(KEY_OUTAGE_CODE),
                                        outageType = inactiveReason.getValue(KEY_OUTAGE_TYPE),
                                        outageDisplayName = inactiveReason.getValue(KEY_NAME),
                                        outageDescription = inactiveReason.getValue(KEY_DESCRIPTION),
                                        userActionRequired = inactiveReason.getValue(
                                            KEY_USER_ACTION_REQUIRED
                                        ),
                                    ).asSuccess()
                                }
                                ?: Failure(NullPointerException(KEY_PUSH_DATA))
                        }
                        TYPE_GEOFENCE_VISIT -> {
                            GeofenceVisitNotification().asSuccess()
                        }
                        else -> {
                            UnknownPushNotificationException(data).asFailure()
                        }
                    }
                }
                data[KEY_VISITS] != null -> {
                    TripUpdateNotification.asSuccess()
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

    private fun handleNotification(notification: PushNotification): Flow<SimpleResult> {
        return suspend {
            when (notification) {
                TripUpdateNotification -> {
                    if (isLoggedInCheck.invoke()) {
                        tripsInteractorProvider.invoke()?.refreshTrips()
                        notificationUtil.sendNotification(
                            context,
                            id = TRIP_NOTIFICATION_ID,
                            text = resourceProvider.stringFromResource(
                                R.string.notification_new_trip
                            ),
                            intentAction = Intent.ACTION_SYNC,
                            type = notification.javaClass.simpleName
                        )
                    } else {
                        JustSuccess
                    }
                }
                is OutageNotification -> {
                    if (isLoggedInCheck.invoke()) {
                        notificationUtil.sendNotification(
                            context,
                            id = getRandomNotificationId(),
                            title = notification.outageDisplayName,
                            text = notification.outageDescription,
                            data = notification,
                            type = notification.javaClass.simpleName,
                            autoCancel = false
                        )
                    } else {
                        JustSuccess
                    }
                }
                is GeofenceVisitNotification -> {
                    // todo
                    JustSuccess
                }
                SdkNotification -> {
                    // ignore SDK notifications
                    JustSuccess
                }
            }
        }.asFlow()
    }

    private fun getRandomNotificationId(): Int {
        return Random.nextInt(1000, Int.MAX_VALUE)
    }

    companion object {
        const val TRIP_NOTIFICATION_ID = 1

        private const val KEY_PUSH_DATA = "data"
        private const val KEY_HYPERTRACK = "hypertrack"
        private const val KEY_NOTIFICATION_TYPE = "type"
        private const val KEY_VISITS = "visits"
        private const val KEY_OUTAGE_TYPE = "type"

        private const val TYPE_OUTAGE = "outage"

        private const val KEY_INACTIVE_REASON = "inactive_reason"
        private const val KEY_NAME = "name"
        private const val KEY_OUTAGE_CODE = "code"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_USER_ACTION_REQUIRED = "user_action_required"

        private const val TYPE_GEOFENCE_VISIT = "geofence_visit"
    }

}

sealed class PushNotification {
    val type: String = javaClass.simpleName
}
object TripUpdateNotification : PushNotification()

@Parcelize
data class OutageNotification(
    val outageCode: String,
    val outageType: String,
    val outageDisplayName: String,
    val outageDescription: String,
    val userActionRequired: String
) : PushNotification(), Parcelable

class GeofenceVisitNotification() : PushNotification()
object SdkNotification : PushNotification()

class UnknownPushNotificationException(data: Map<String, String>) : Exception(data.toString())
