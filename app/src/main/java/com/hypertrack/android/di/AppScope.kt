package com.hypertrack.android.di

import android.content.Context
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.LiveAccountApi
import com.hypertrack.android.api.api_interface.AppBackendApi
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.FileRepository
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.MyPreferences
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.repository.PublishableKeyRepository
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.ui.common.delegates.DeviceStatusMarkerDisplayDelegate
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceVisitAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.delegates.display.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.delegates.display.GeotagDisplayDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.use_case.app.threading.ActionsScope
import com.hypertrack.android.use_case.app.threading.EffectsScope
import com.hypertrack.android.use_case.sdk.TrackingState
import com.hypertrack.android.utils.BatteryLevelMonitor
import com.hypertrack.android.utils.CognitoAccountLoginProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.ImageDecoder
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.CognitoExchangeTokenApi
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class AppScope(
    val appContext: Context,
    // interactors
    val geocodingInteractor: GeocodingInteractor,
    // repositories
    val accessTokenRepository: AccessTokenRepository,
    val publishableKeyRepository: PublishableKeyRepository,
    val userRepository: UserRepository,
    val preferencesRepository: PreferencesRepository,
    val fileRepository: FileRepository,
    val measurementUnitsRepository: MeasurementUnitsRepository,
    // delegates
    val orderAddressDelegate: OrderAddressDelegate,
    val geofenceAddressDelegate: GeofenceAddressDelegate,
    val geofenceVisitAddressDelegate: GeofenceVisitAddressDelegate,
    val graphQlGeofenceVisitAddressDelegate: GraphQlGeofenceVisitAddressDelegate,
    val geofenceVisitDisplayDelegate: GeofenceVisitDisplayDelegate,
    val deviceStatusMarkerDisplayDelegate: DeviceStatusMarkerDisplayDelegate,
    val geotagDisplayDelegate: GeotagDisplayDelegate,
    val geofenceNameDelegate: GeofenceNameDelegate,
    // misc
    val branchWrapper: BranchWrapper,
    val cognitoAccountLoginProvider: CognitoAccountLoginProvider,
    val appBackendApi: AppBackendApi,
    val tokenApi: CognitoExchangeTokenApi,
    val liveAccountApi: LiveAccountApi,
    val myPreferences: MyPreferences,
    val crashReportsProvider: CrashReportsProvider,
    val osUtilsProvider: OsUtilsProvider,
    val resourceProvider: ResourceProvider,
    val dateTimeFormatter: DateTimeFormatter,
    val distanceFormatter: DistanceFormatter,
    val timeFormatter: TimeValueFormatter,
    val placesClient: PlacesClient,
    val imageDecoder: ImageDecoder,
    val mapItemsFactory: HypertrackMapItemsFactory,
    val batteryLevelMonitor: BatteryLevelMonitor,
    val notificationUtil: NotificationUtil,
    val moshi: Moshi,
    val trackingStateListener: (TrackingState) -> Unit,
    // threading
    val actionsScope: ActionsScope,
    val effectsScope: EffectsScope
) {

    override fun toString(): String = javaClass.simpleName
}
