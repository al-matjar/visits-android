package com.hypertrack.android.messaging

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.sdk.logger.HTLogger
import com.hypertrack.sdk.pipelines.PushTokenError
import com.hypertrack.sdk.pipelines.PushTokenSuccess
import com.hypertrack.sdk.pipelines.RefreshPushTokenStep
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class VisitsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
//        Log.v("hypertrack-verbose", "Got firebase token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            MyApplication.injector.getPushReceiver().onPushReceived(this, remoteMessage)
        } catch (e: Exception) {
            Injector.crashReportsProvider
        }
    }

    companion object {
        //use only for debug
        suspend fun getFirebaseToken(): String = suspendCoroutine {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    it.resume(token)
                }
                .addOnFailureListener { e ->
                    it.resume(e.toString())
                }
        }

    }
}