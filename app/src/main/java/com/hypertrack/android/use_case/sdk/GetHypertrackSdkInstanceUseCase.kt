package com.hypertrack.android.use_case.sdk

import android.util.Log
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.utils.TrackingStateValue
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.ServiceNotificationConfig
import com.hypertrack.sdk.TrackingError
import com.hypertrack.sdk.TrackingStateObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.security.PrivateKey

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class GetHypertrackSdkInstanceUseCase {

    fun execute(publishableKey: PublishableKey): Flow<HyperTrack> {
        return HyperTrack.getInstance(publishableKey.value).toFlow()
    }

}
