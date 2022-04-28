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
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.catchException
import com.hypertrack.sdk.logger.HTLogger
import com.hypertrack.sdk.pipelines.PushTokenError
import com.hypertrack.sdk.pipelines.PushTokenSuccess
import com.hypertrack.sdk.pipelines.RefreshPushTokenStep
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.runBlocking
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
        runBlocking {
            Injector.getHandlePushUseCase().execute(remoteMessage)
                .catchException { Injector.crashReportsProvider.logException(it) }
                .collect()
        }
    }

    companion object {
        //use only for debug
        suspend fun getFirebaseToken(): String = suspendCoroutine {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (MyApplication.DEBUG_MODE) {
                        Log.v(
                            VisitsMessagingService::class.java.simpleName,
                            "Got Firebase token: $token"
                        )
                    }
                    it.resume(token)
                }
                .addOnFailureListener { e ->
                    it.resume(e.toString())
                }
        }

    }
}
