package com.hypertrack.android.utils

import android.content.Context
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.*
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.common.ParamViewModelFactory
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.ViewModelFactory
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.*
import com.hypertrack.android.utils.formatters.*
import com.hypertrack.logistics.android.github.R
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.ServiceNotificationConfig
import com.hypertrack.sdk.views.HyperTrackViews
import com.squareup.moshi.Moshi
import com.squareup.moshi.recipes.RuntimeJsonAdapterFactory
import com.squareup.moshi.recipes.ZonedDateTimeJsonAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Provider


class ServiceLocator(val crashReportsProvider: CrashReportsProvider) {

    fun getAccessTokenRepository(deviceId: String, userName: String) =
        BasicAuthAccessTokenRepository(AUTH_URL, deviceId, userName)

    fun getHyperTrackService(publishableKey: String): HyperTrackService {
        val listener = TrackingState(crashReportsProvider)
        val sdkInstance = HyperTrack
            .getInstance(publishableKey)
            .addTrackingListener(listener)
            .backgroundTrackingRequirement(false)
            .setTrackingNotificationConfig(
                ServiceNotificationConfig.Builder()
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .build()
            )

        return HyperTrackService(listener, sdkInstance, crashReportsProvider)
    }

}


object Injector {

    val crashReportsProvider: CrashReportsProvider by lazy { FirebaseCrashReportsProvider() }

    private val appScope: AppScope by lazy { createAppScope() }
    private var userScope: UserScope? = null
    var tripCreationScope: TripCreationScope? = null

    val deeplinkProcessor: DeeplinkProcessor = BranchIoDeepLinkProcessor(crashReportsProvider)

    private val serviceLocator = ServiceLocator(crashReportsProvider)

    val batteryLevelMonitor = BatteryLevelMonitor(crashReportsProvider)

    private fun createAppScope(): AppScope {
        val context = MyApplication.context
        val crashReportsProvider = this.crashReportsProvider
        val osUtilsProvider = OsUtilsProvider(context, crashReportsProvider)
        return AppScope(
            crashReportsProvider,
            osUtilsProvider,
            DateTimeFormatterImpl(),
            LocalizedDistanceFormatter(osUtilsProvider),
            TimeFormatterImpl(osUtilsProvider)
        )
    }

    fun getMoshi(): Moshi = Moshi.Builder()
        .add(HistoryCoordinateJsonAdapter())
        .add(GeometryJsonAdapter())
        .add(ZonedDateTimeJsonAdapter())
        .add(
            RuntimeJsonAdapterFactory(HistoryMarker::class.java, "type")
                .registerSubtype(HistoryStatusMarker::class.java, "device_status")
                .registerSubtype(HistoryTripMarker::class.java, "trip_marker")
                .registerSubtype(HistoryGeofenceMarker::class.java, "geofence")
        )
        .build()

    fun provideViewModelFactory(context: Context): ViewModelFactory {
        return ViewModelFactory(
            getAccountRepo(context),
            getDriverRepo(),
            crashReportsProvider,
            getPermissionInteractor(),
            getLoginInteractor(),
            getOsUtilsProvider(MyApplication.context),
            getMoshi()
        )
    }

    //todo user scope
    fun <T> provideParamVmFactory(param: T): ParamViewModelFactory<T> {
        return ParamViewModelFactory(
            param,
            appScope,
            { getUserScope() },
            getOsUtilsProvider(MyApplication.context),
            getAccountRepo(MyApplication.context),
            getMoshi(),
            crashReportsProvider,
            getDeviceLocationProvider()
        )
    }

    fun provideUserScopeViewModelFactory(): UserScopeViewModelFactory {
        return getUserScope().userScopeViewModelFactory
    }

    fun provideTabs(): List<Tab> = mutableListOf<Tab>().apply {
        addAll(
            listOf(
                Tab.CURRENT_TRIP,
                Tab.HISTORY,
            )
        )
        add(Tab.ORDERS)
        addAll(
            listOf(
                Tab.PLACES,
                Tab.SUMMARY,
                Tab.PROFILE,
            )
        )
    }

    private fun createUserScope(
        publishableKey: String,
        accountRepository: AccountRepository,
        accessTokenRepository: BasicAuthAccessTokenRepository,
        driverRepository: DriverRepository,
        permissionsInteractor: PermissionsInteractor,
        deviceLocationProvider: DeviceLocationProvider,
        osUtilsProvider: OsUtilsProvider,
        crashReportsProvider: CrashReportsProvider,
        moshi: Moshi,
        myPreferences: MyPreferences,
        fileRepository: FileRepository,
        imageDecoder: ImageDecoder,
    ): UserScope {
        val deviceId = accessTokenRepository.deviceId

        val apiClient = ApiClient(
            accessTokenRepository,
            BASE_URL,
            deviceId,
            moshi,
            crashReportsProvider
        )
        val historyRepository = HistoryRepositoryImpl(
            apiClient,
            crashReportsProvider,
            osUtilsProvider
        )
        val scope = CoroutineScope(Dispatchers.IO)
        val placesRepository = PlacesRepositoryImpl(
            deviceId,
            apiClient,
            moshi,
            osUtilsProvider,
            crashReportsProvider
        )
        val integrationsRepository = IntegrationsRepositoryImpl(apiClient)
        val placesInteractor = PlacesInteractorImpl(
            placesRepository,
            integrationsRepository,
            osUtilsProvider,
            appScope.datetimeFormatter,
            Intersect(),
            GlobalScope
        )
        val hyperTrackService = serviceLocator.getHyperTrackService(publishableKey)

        val photoUploadQueueInteractor = PhotoUploadQueueInteractorImpl(
            myPreferences,
            fileRepository,
            crashReportsProvider,
            imageDecoder,
            apiClient,
            scope,
            RetryParams(
                retryTimes = 3,
                initialDelay = 1000,
                factor = 10.0,
                maxDelay = 30 * 1000
            )
        )

        val tripsRepository = TripsRepositoryImpl(
            apiClient,
            myPreferences,
            hyperTrackService,
            GlobalScope,
            accountRepository.isPickUpAllowed
        )

        val tripsInteractor = TripsInteractorImpl(
            tripsRepository,
            apiClient,
            hyperTrackService,
            photoUploadQueueInteractor,
            imageDecoder,
            osUtilsProvider,
            Dispatchers.IO,
            GlobalScope
        )

        val feedbackInteractor = FeedbackInteractor(
            accessTokenRepository.deviceId,
            tripsInteractor,
            integrationsRepository,
            moshi,
            osUtilsProvider,
            crashReportsProvider
        )

        val placesVisitsInteractor = PlacesVisitsInteractorImpl(
            placesRepository
        )

        val historyInteractor = HistoryInteractorImpl(
            historyRepository,
            GlobalScope
        )

        val googlePlacesInteractor = GooglePlacesInteractorImpl(
            placesClient
        )

        val userScope = UserScope(
            tripsInteractor,
            TripsUpdateTimerInteractor(tripsInteractor),
            placesInteractor,
            placesVisitsInteractor,
            googlePlacesInteractor,
            historyInteractor,
            feedbackInteractor,
            integrationsRepository,
            hyperTrackService,
            photoUploadQueueInteractor,
            apiClient,
            UserScopeViewModelFactory(
                appScope,
                { getUserScope() },
                tripsInteractor,
                placesInteractor,
                feedbackInteractor,
                integrationsRepository,
                driverRepository,
                accountRepository,
                crashReportsProvider,
                hyperTrackService,
                permissionsInteractor,
                accessTokenRepository,
                apiClient,
                osUtilsProvider,
                placesClient,
                deviceLocationProvider,
            )
        )

        crashReportsProvider.setUserIdentifier(
            moshi.adapter(UserIdentifier::class.java).toJson(
                UserIdentifier(
                    deviceId = accessTokenRepository.deviceId,
                    driverId = driverRepository.username,
                    pubKey = accountRepository.publishableKey,
                )
            )
        )

        return userScope
    }

    private fun getUserScope(): UserScope {
        if (userScope == null) {
            val myPreferences = getMyPreferences(MyApplication.context)
            val publishableKey = myPreferences.getAccountData().publishableKey
                ?: throw IllegalStateException("No publishableKey saved")

            userScope = createUserScope(
                publishableKey,
                getAccountRepo(MyApplication.context),
                accessTokenRepository(MyApplication.context),
                getDriverRepo(),
                getPermissionInteractor(),
                getDeviceLocationProvider(),
                getOsUtilsProvider(MyApplication.context),
                crashReportsProvider,
                getMoshi(),
                getMyPreferences(MyApplication.context),
                getFileRepository(),
                getImageDecoder()
            )
        }
        return userScope!!
    }

    private fun destroyUserScope() {
        userScope?.onDestroy()
        userScope = null
    }

    private val placesClient: PlacesClient by lazy {
        Places.createClient(MyApplication.context)
    }

    private fun getDriverRepo(): DriverRepository {
        return DriverRepository(
            getAccountRepo(MyApplication.context),
            serviceLocator,
            getMyPreferences(MyApplication.context),
            getOsUtilsProvider(MyApplication.context),
            crashReportsProvider,
        )
    }

    private fun getFileRepository(): FileRepository {
        return FileRepositoryImpl()
    }

    private fun getPermissionInteractor() =
        PermissionsInteractorImpl { getUserScope().hyperTrackService }

    private val tokenForPublishableKeyExchangeService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LIVE_API_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .build()
        return@lazy retrofit.create(TokenForPublishableKeyExchangeService::class.java)
    }

    private val liveAccountUrlService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LIVE_ACCOUNT_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .build()
        return@lazy retrofit.create(LiveAccountApi::class.java)
    }

    private fun getLoginInteractor(): LoginInteractor {
        return LoginInteractorImpl(
            getDriverRepo(),
            getCognitoLoginProvider(MyApplication.context),
            getAccountRepo(MyApplication.context),
            tokenForPublishableKeyExchangeService,
            liveAccountUrlService,
            MyApplication.SERVICES_API_KEY
        )
    }

    private fun getMyPreferences(context: Context): MyPreferences =
        MyPreferences(context, getMoshi())

    fun accessTokenRepository(context: Context): BasicAuthAccessTokenRepository =
        (getMyPreferences(context).restoreRepository()
            ?: throw IllegalStateException("No access token repository was saved"))

    private fun getAccountRepo(context: Context) =
        AccountRepository(serviceLocator, getAccountData(context), getMyPreferences(context))
        { destroyUserScope() }

    private fun getAccountData(context: Context): AccountData =
        getMyPreferences(context).getAccountData()

    fun getOsUtilsProvider(context: Context): OsUtilsProvider {
        return OsUtilsProvider(context, crashReportsProvider)
    }

    private fun getImageDecoder(): ImageDecoder = SimpleImageDecoder()

    private fun getCognitoLoginProvider(context: Context): CognitoAccountLoginProvider =
        CognitoAccountLoginProviderImpl(context)

    private fun getDeviceLocationProvider(): DeviceLocationProvider {
        return FusedDeviceLocationProvider(MyApplication.context)
    }

}

class TripCreationScope(
    val destinationData: DestinationData
)

class UserScope(
    val tripsInteractor: TripsInteractor,
    val tripsUpdateTimerInteractor: TripsUpdateTimerInteractor,
    val placesInteractor: PlacesInteractor,
    val placesVisitsInteractor: PlacesVisitsInteractor,
    val googlePlacesInteractor: GooglePlacesInteractor,
    val historyInteractor: HistoryInteractor,
    val feedbackInteractor: FeedbackInteractor,
    val integrationsRepository: IntegrationsRepository,
    val hyperTrackService: HyperTrackService,
    val photoUploadQueueInteractor: PhotoUploadQueueInteractor,
    val apiClient: ApiClient,
    val userScopeViewModelFactory: UserScopeViewModelFactory
) {
    fun onDestroy() {
        tripsUpdateTimerInteractor.onDestroy()
    }
}

//todo move app scope dependencies here
class AppScope(
    val crashReportsProvider: CrashReportsProvider,
    val osUtilsProvider: OsUtilsProvider,
    val datetimeFormatter: DatetimeFormatter,
    val distanceFormatter: DistanceFormatter,
    val timeFormatter: TimeFormatter,
)

fun interface Factory<A, T> {
    fun create(a: A): T
}

interface AccountPreferencesProvider {
    var wasWhitelisted: Boolean
    val isManualCheckInAllowed: Boolean
    val isPickUpAllowed: Boolean
    var shouldStartTracking: Boolean
}

const val BASE_URL = "https://live-app-backend.htprod.hypertrack.com/"
const val LIVE_API_URL_BASE = "https://live-api.htprod.hypertrack.com/"
const val AUTH_URL = LIVE_API_URL_BASE + "authenticate"
const val MAX_IMAGE_SIDE_LENGTH_PX = 1024

const val LIVE_ACCOUNT_URL_BASE = "https://live-account.htprod.hypertrack.com"
