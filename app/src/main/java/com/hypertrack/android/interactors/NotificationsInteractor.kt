package com.hypertrack.android.interactors

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R

class NotificationsInteractor {

    private val notificationManager by lazy {
        MyApplication.context.getSystemService(
            AppCompatActivity.NOTIFICATION_SERVICE
        ) as NotificationManager
    }

    fun sendNewTripNotification(context: Context) {
        val notificationIntent = Intent(
            MyApplication.context,
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SYNC
        }

        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val contentIntent = PendingIntent.getActivity(
            MyApplication.context,
            ProgressDialogFragment.TRIP_NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    Notification.Builder(context, MyApplication.IMPORTANT_CHANNEL_ID)
                else
                    Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
                )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setContentText(R.string.notification_new_trip.stringFromResource())
            .setSmallIcon(R.drawable.ic_stat_notification)
            .build()
        notificationManager.notify(ProgressDialogFragment.TRIP_NOTIFICATION_ID, notification)
    }


}