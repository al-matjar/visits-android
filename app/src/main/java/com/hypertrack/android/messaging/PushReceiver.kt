package com.hypertrack.android.messaging

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.interactors.NotificationsInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.createStringMapAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Provider

class PushReceiver(
    private val accountRepoProvider: Provider<AccountRepository>,
    private val tripsInteractorProvider: Provider<TripsInteractor>,
    private val notificationsInteractor: NotificationsInteractor,
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi
) {

    fun onPushReceived(context: Context, remoteMessage: RemoteMessage) {
        val data: Map<String, String> = if (remoteMessage.data.isNotEmpty()) {
            remoteMessage.data
        } else try {
            // this is for debug only, you can send a test message from Firebase admin panel with text
            // that contains JSON that mimics the format of real message payload
            remoteMessage.notification?.body?.let {
                moshi.createStringMapAdapter().fromJson(it)
            }!!
        } catch (e: Exception) {
            mapOf()
        }

//        Log.d("hypertrack-verbose", "Got push ${data}")
        if (data["visits"] != null) {
            if (accountRepoProvider.get().isLoggedIn) {
                notificationsInteractor.sendNewTripNotification(context)
                GlobalScope.launch {
                    try {
                        tripsInteractorProvider.get().refreshTrips()
                    } catch (e: Exception) {
                        crashReportsProvider.logException(e)
                    }
                }
            }
        }
    }

}
