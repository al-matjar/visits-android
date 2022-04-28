package com.hypertrack.android.utils

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.logistics.android.github.R

object NotificationUtil {

    const val KEY_NOTIFICATION_DATA = "notification_data"
    const val KEY_NOTIFICATION_TYPE = "notification_type"

    fun sendNotification(
        context: Context,
        id: Int,
        type: String,
        text: String,
        title: String? = null,
        data: Parcelable? = null,
        intentAction: String? = null,
        autoCancel: Boolean = true
    ): SimpleResult {
        return tryAsSimpleResult {
            val notificationManager = context.getSystemService(
                AppCompatActivity.NOTIFICATION_SERVICE
            ) as NotificationManager

            val notificationIntent = Intent(
                context,
                MainActivity::class.java
            ).also { intent: Intent ->
                intent.putExtra(KEY_NOTIFICATION_TYPE, type)
                intentAction?.let { intent.action = it }
                data?.let {
                    intent.putExtra(KEY_NOTIFICATION_DATA, it)
                }
            }

            notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            val contentIntent = PendingIntent.getActivity(
                context,
                id,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            val notification = (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        Notification.Builder(context, MyApplication.IMPORTANT_CHANNEL_ID)
                    else
                        Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
                    )
                .setAutoCancel(autoCancel)
                .setContentIntent(contentIntent)
                .setContentText(text)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .build()

            notificationManager.notify(id, notification)
        }
    }

    fun setUpNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                MyApplication.CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_description)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(
                MyApplication.IMPORTANT_CHANNEL_ID,
                context.getString(R.string.notification_important_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_important_channel_description)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

}
