package com.hypertrack.android.messaging

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.PushReceivedAction
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.toSuspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class VisitsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (MyApplication.DEBUG_MODE) {
            Log.v(javaClass.simpleName, "Got Firebase token: $token")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Injector.provideAppInteractor().handleAction(
            PushReceivedAction(remoteMessage)
        )
    }

    companion object {
        // use only for debug
        suspend fun getFirebaseToken(): Result<String> {
            return FirebaseMessaging.getInstance().token.toSuspendCoroutine().also {
                if (MyApplication.DEBUG_MODE) {
                    Log.v(
                        VisitsMessagingService::class.java.simpleName,
                        "Got Firebase token: $it"
                    )
                }
            }
        }
    }
}
