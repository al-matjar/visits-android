package com.hypertrack.android.mock

import android.content.Context
import com.google.android.a.c
import com.hypertrack.android.interactors.NotificationsInteractor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MockNotifications(
    private val context: Context,
    private val notificationsInteractor: NotificationsInteractor
) {

    fun sendTripNotification() {
        GlobalScope.launch {
            delay(3000)
            notificationsInteractor.sendNewTripNotification(context)
        }
    }

}


