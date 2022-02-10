package com.hypertrack.android.interactors

import android.app.Activity
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.repository.AccessTokenRepository
import com.hypertrack.android.repository.BasicAuthAccessTokenRepository
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.repository.IntegrationsRepositoryImpl
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class FeedbackInteractor(
    private val deviceId: DeviceId,
    private val tripsInteractor: TripsInteractor,
    private val integrationsRepository: IntegrationsRepositoryImpl,
    private val moshi: Moshi,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider
) {
    var savedFeedbackText: String = ""

    fun sendFeedback(activity: Activity, text: String) {
        logAppState()
        val data = mapOf(
            "Device ID" to deviceId,
            "App version" to osUtilsProvider.getBuildVersion()
        ).map { "${it.key}:\t${it.value}" }.joinToString("\n")
        osUtilsProvider.mailTo(
            activity,
            email = osUtilsProvider.stringFromResource(R.string.feedback_email),
            subject = osUtilsProvider.stringFromResource(R.string.feedback_subject),
            text = osUtilsProvider.stringFromResource(R.string.feedback_email_text, data, text),
        )
        crashReportsProvider.log(text)
        crashReportsProvider.logException(ManuallyTriggeredException)
    }

    private fun logAppState() {
        try {
            val tripsState = tripsInteractor
            //clear app state from any sensitive data (location, metadata, addresses, notes, etc)
            val appState: Map<String, Any?> = mutableMapOf(
                "trips" to tripsInteractor.logState(),
                "integrations" to integrationsRepository.logState()
            )
            crashReportsProvider.log(
                moshi.adapter<Map<String, Any?>>(
                    Types.newParameterizedType(
                        Map::class.java, String::class.java,
                        Any::class.java
                    )
                ).toJson(appState)
            )
        } catch (e: Exception) {
            if (MyApplication.DEBUG_MODE) {
                throw e
            }
        }
    }
}

object ManuallyTriggeredException : Exception()
