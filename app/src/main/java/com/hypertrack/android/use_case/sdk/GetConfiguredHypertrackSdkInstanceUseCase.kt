package com.hypertrack.android.use_case.sdk

import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.logistics.android.github.R
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.ServiceNotificationConfig
import com.hypertrack.sdk.TrackingError
import com.hypertrack.sdk.TrackingStateObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class GetConfiguredHypertrackSdkInstanceUseCase(
    private val trackingStateListener: (TrackingState) -> Unit,
    private val getHypertrackSdkInstanceUseCase: GetHypertrackSdkInstanceUseCase,
) {

    fun execute(publishableKey: PublishableKey): Flow<HyperTrack> {
        return getHypertrackSdkInstanceUseCase.execute(publishableKey).map { hypertrackSdk ->
            val listener = createListener()
            hypertrackSdk
                .addTrackingListener(listener)
                .backgroundTrackingRequirement(false)
                .setTrackingNotificationConfig(
                    ServiceNotificationConfig.Builder()
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .build()
                )
        }
    }

    private fun createListener(): TrackingStateObserver.OnTrackingStateChangeListener {
        return object : TrackingStateObserver.OnTrackingStateChangeListener {
            override fun onTrackingStart() {
                trackingStateListener.invoke(TrackingStarted)
            }

            override fun onTrackingStop() {
                trackingStateListener.invoke(TrackingStopped)
            }

            override fun onError(trackingError: TrackingError) {
                when (trackingError.code) {
                    TrackingError.AUTHORIZATION_ERROR -> {
                        if (trackingError.message.contains("trial ended")) {
                            DeviceDeleted
                        } else {
                            trackingFailure(trackingError, "")
                        }
                    }
                    TrackingError.PERMISSION_DENIED_ERROR -> {
                        PermissionsDenied
                    }
                    TrackingError.DATA_STORE_ERROR -> {
                        trackingFailure(trackingError, "DATA_STORE_ERROR")
                    }
                    TrackingError.GPS_PROVIDER_DISABLED_ERROR -> {
                        LocationServicesDisabled
                    }
                    TrackingError.INVALID_PUBLISHABLE_KEY_ERROR -> {
                        trackingFailure(trackingError, "INVALID_PUBLISHABLE_KEY_ERROR")
                    }
                    TrackingError.UNKNOWN_ERROR -> {
                        trackingFailure(trackingError, "UNKNOWN_ERROR")
                    }
                    TrackingError.UNKNOWN_NETWORK_ERROR -> {
                        trackingFailure(trackingError, "UNKNOWN_NETWORK_ERROR")
                    }
                    else -> {
                        trackingFailure(trackingError, "")
                    }
                }.let {
                    trackingStateListener.invoke(it)
                }
            }
        }
    }

    private fun trackingFailure(trackingError: TrackingError, codeName: String): TrackingFailure {
        return TrackingFailure(
            trackingError.code,
            message = trackingError.message,
            codeName = codeName
        )
    }

}
