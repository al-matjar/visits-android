package com.hypertrack.android.di

import com.hypertrack.android.di.Injector.crashReportsProvider
import io.mockk.mockk
import junit.framework.TestCase

class AppScopeTest {
    companion object {
        fun appScope(): AppScope {
            return AppScope(
                appContext = mockk(),
                geocodingInteractor = mockk(),
                accessTokenRepository = mockk(),
                publishableKeyRepository = mockk(),
                userRepository = mockk(),
                preferencesRepository = mockk(),
                fileRepository = mockk(),
                measurementUnitsRepository = mockk(),
                orderAddressDelegate = mockk(),
                geofenceAddressDelegate = mockk(),
                geofenceVisitAddressDelegate = mockk(),
                graphQlGeofenceVisitAddressDelegate = mockk(),
                geofenceVisitDisplayDelegate = mockk(),
                deviceStatusMarkerDisplayDelegate = mockk(),
                geotagDisplayDelegate = mockk(),
                geofenceNameDelegate = mockk(),
                branchWrapper = mockk(),
                cognitoAccountLoginProvider = mockk(),
                appBackendApi = mockk(),
                tokenApi = mockk(),
                liveAccountApi = mockk(),
                myPreferences = mockk(),
                crashReportsProvider = mockk(),
                osUtilsProvider = mockk(),
                resourceProvider = mockk(),
                dateTimeFormatter = mockk(),
                distanceFormatter = mockk(),
                timeFormatter = mockk(),
                placesClient = mockk(),
                imageDecoder = mockk(),
                mapItemsFactory = mockk(),
                batteryLevelMonitor = mockk(),
                notificationUtil = mockk(),
                moshi = mockk(),
                trackingStateListener = mockk(),
                appCoroutineScope = mockk(),
                stateMachineContext = mockk()
            )
        }
    }
}