package com.hypertrack.android.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import com.hypertrack.android.di.Injector
import com.hypertrack.logistics.android.github.BuildConfig

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        Log.w("hypertrack-verbose", "Visits app started")
        newInjector.appOnCreate(this)
    }

    override fun toString(): String = javaClass.simpleName

    companion object {
        const val CHANNEL_ID = "default_notification_channel"
        const val IMPORTANT_CHANNEL_ID = "important_notification_channel"
        const val SERVICES_API_KEY = BuildConfig.SERVICES_API_KEY
        const val GRAPHQL_API_KEY = BuildConfig.GRAPHQL_API_KEY

        val DEBUG_MODE = BuildConfig.DEBUG

        val newInjector: Injector = Injector

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}
