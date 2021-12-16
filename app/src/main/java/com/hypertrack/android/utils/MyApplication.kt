package com.hypertrack.android.utils

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.libraries.places.api.Places
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import java.util.*

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        context = this

        injector.deeplinkProcessor.appOnCreate(this)

        if (BuildConfig.DEBUG) {
//            HyperTrack.enableDebugLogging()
        }

        Places.initialize(
            applicationContext,
            getString(R.string.google_places_api_key),
            Locale.getDefault()
        );

        buildNotificationChannels()

        injector.batteryLevelMonitor.init(this)

        Log.w("hypertrack-verbose", "Visits app started")

        upgradeSecurityProvider(this, injector.crashReportsProvider);
    }

    private fun buildNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(
                IMPORTANT_CHANNEL_ID,
                getString(R.string.notification_important_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_important_channel_description)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    private fun upgradeSecurityProvider(
        context: Context,
        crashReportsProvider: CrashReportsProvider
    ) {
        try {
            ProviderInstaller.installIfNeededAsync(
                this,
                object : ProviderInstaller.ProviderInstallListener {
                    override fun onProviderInstalled() {
                        crashReportsProvider.log("Security Provider installed")
                    }

                    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                        try {
                            GoogleApiAvailability.getInstance()
                                .showErrorNotification(context, errorCode)
                            crashReportsProvider.logException(
                                Exception("Security provider installation failed, error code: $errorCode")
                            )
                        } catch (e: Exception) {
                            crashReportsProvider.logException(e)
                        }
                    }
                })
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    companion object {
        const val TAG = "MyApplication"
        const val CHANNEL_ID = "default_notification_channel"
        const val IMPORTANT_CHANNEL_ID = "important_notification_channel"
        const val SERVICES_API_KEY = BuildConfig.SERVICES_API_KEY
        const val GRAPHQL_API_KEY = BuildConfig.GRAPHQL_API_KEY

        //should be used only in Injector and MainActivity
        val RECORDING_MODE: Boolean = if (BuildConfig.DEBUG) {
            BuildConfig.RECORDING_MODE
        } else {
            false
        }

        //should be used only in Injector and MainActivity
        val MOCK_MODE: Boolean = if (BuildConfig.DEBUG) {
            RECORDING_MODE || BuildConfig.MOCK_MODE
        } else {
            false
        }

        val DEBUG_MODE = BuildConfig.DEBUG

        val injector: Injector = Injector

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}
