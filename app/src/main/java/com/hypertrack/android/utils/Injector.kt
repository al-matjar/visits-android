package com.hypertrack.android.utils

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.*
import com.hypertrack.android.api.graphql.GraphQlApi
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerActiveData
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerData
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerInactiveData
import com.hypertrack.android.interactors.*
import com.hypertrack.android.interactors.history.GraphQlHistoryInteractor
import com.hypertrack.android.interactors.history.HistoryInteractor
import com.hypertrack.android.messaging.PushReceiver
import com.hypertrack.android.repository.*
import com.hypertrack.android.deeplink.BranchIoDeepLinkProcessor
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.deeplink.DeeplinkProcessor
import com.hypertrack.android.ui.common.ParamViewModelFactory
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.ViewModelFactory
import com.hypertrack.android.ui.common.delegates.DateTimeRangeFormatterDelegate
import com.hypertrack.android.ui.common.delegates.DeviceStatusMarkerDisplayDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceVisitAddressDelegate
import com.hypertrack.android.ui.common.delegates.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.delegates.GeotagDisplayDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.*
import com.hypertrack.android.utils.formatters.*
import com.hypertrack.logistics.android.github.R
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.ServiceNotificationConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.recipes.RuntimeJsonAdapterFactory
import com.squareup.moshi.recipes.ZonedDateTimeJsonAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors.newSingleThreadExecutor
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext


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

    //should be static to enable reliable exception reporting in all scopes
    val crashReportsProvider: CrashReportsProvider by lazy { FirebaseCrashReportsProvider() }

    private val appScope: AppScope by lazy { createAppScope(MyApplication.context) }
    private var userScope: UserScope? = null
    var tripCreationScope: TripCreationScope? = null

    val deeplinkProcessor by lazy { appScope.deeplinkProcessor }

    private val serviceLocator = ServiceLocator(crashReportsProvider)

    val batteryLevelMonitor = BatteryLevelMonitor(crashReportsProvider)

    private fun createAppScope(context: Context): AppScope {
        val crashReportsProvider = this.crashReportsProvider
        val serviceLocator = serviceLocator
        val osUtilsProvider = OsUtilsProvider(context, crashReportsProvider)
        val accountRepositoryProvider = { getAccountRepo(context) }
        val driverRepository = DriverRepository(
            accountRepositoryProvider,
            serviceLocator,
            getMyPreferences(MyApplication.context),
            osUtilsProvider,
            crashReportsProvider,
        )
        val moshi = getMoshi()
        val datetimeFormatter = DateTimeFormatterImpl()
        val distanceFormatter = LocalizedDistanceFormatter(osUtilsProvider)
        val timeFormatter = TimeValueFormatterImpl(osUtilsProvider)
        val geofenceVisitAddressDelegate = GeofenceVisitAddressDelegate(osUtilsProvider)
        val geofenceAddressDelegate = GeofenceAddressDelegate(osUtilsProvider)
        val datetimeRangeFormatterDelegate = DateTimeRangeFormatterDelegate(
            osUtilsProvider,
            datetimeFormatter
        )
        val loginInteractor = LoginInteractorImpl(
            driverRepository,
            getCognitoLoginProvider(MyApplication.context),
            crashReportsProvider,
            moshi,
            accountRepositoryProvider,
            serviceLocator,
            tokenForPublishableKeyExchangeService,
            liveAccountUrlService,
            MyApplication.SERVICES_API_KEY
        )
        return AppScope(
            MyApplication.context,
            driverRepository,
            DeeplinkInteractor(
                driverRepository,
                accountRepositoryProvider,
                crashReportsProvider,
                serviceLocator,
                moshi
            ),
            NotificationsInteractor(),
            loginInteractor,
            crashReportsProvider,
            osUtilsProvider,
            datetimeFormatter,
            distanceFormatter,
            timeFormatter,
            geofenceAddressDelegate,
            GeofenceVisitDisplayDelegate(
                osUtilsProvider,
                datetimeFormatter,
                distanceFormatter,
                timeFormatter,
                geofenceVisitAddressDelegate,
                datetimeRangeFormatterDelegate
            ),
            DeviceStatusMarkerDisplayDelegate(
                osUtilsProvider,
                distanceFormatter,
                timeFormatter,
                datetimeRangeFormatterDelegate,
            ),
            GeotagDisplayDelegate(
                osUtilsProvider,
                datetimeFormatter,
                moshi
            ),
            BranchIoDeepLinkProcessor(
                Injector.crashReportsProvider,
                osUtilsProvider,
                BranchWrapper()
            ),
            HypertrackMapItemsFactory(osUtilsProvider),
            accountRepositoryProvider,
            moshi,
            CoroutineScope(SupervisorJob()),
            newSingleThreadExecutor().asCoroutineDispatcher()
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
        .add(
            RuntimeJsonAdapterFactory(GraphQlDeviceStatusMarkerData::class.java, "__typename")
                .registerSubtype(
                    GraphQlDeviceStatusMarkerActiveData::class.java,
                    "DeviceStatusMarkerActive"
                )
                .registerSubtype(
                    GraphQlDeviceStatusMarkerInactiveData::class.java,
                    "DeviceStatusMarkerInactive"
                )
        )
        .build()

    fun provideViewModelFactory(context: Context): ViewModelFactory {
        return ViewModelFactory(
            appScope,
            getPermissionInteractor(),
            crashReportsProvider,
            getOsUtilsProvider(MyApplication.context),
            appScope.moshi
        )
    }

    //todo move to user scope
    fun <T> provideParamVmFactory(param: T): ParamViewModelFactory<T> {
        return ParamViewModelFactory(
            param,
            appScope,
            { getUserScope() },
            getOsUtilsProvider(MyApplication.context),
            appScope.moshi,
            crashReportsProvider
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
        appScope: AppScope,
        accessTokenRepository: BasicAuthAccessTokenRepository,
        permissionsInteractor: PermissionsInteractor,
        osUtilsProvider: OsUtilsProvider,
        crashReportsProvider: CrashReportsProvider,
        moshi: Moshi,
        myPreferences: MyPreferences,
        fileRepository: FileRepository,
        imageDecoder: ImageDecoder,
    ): UserScope {
        val deviceId = DeviceId(accessTokenRepository.deviceId)
        val deviceLocationProvider =
            FusedDeviceLocationProvider(appScope.appContext, crashReportsProvider)
        val api = createRemoteApi(
            BASE_URL,
            moshi,
            accessTokenRepository,
            crashReportsProvider
        )
        val apiClient = ApiClient(
            api,
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
            appScope.dateTimeFormatter,
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
            false
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
            deviceId,
            tripsInteractor,
            integrationsRepository,
            moshi,
            osUtilsProvider,
            crashReportsProvider
        )

        val graphQlApi = createGraphQlApi(GRAPHQL_API_URL, moshi)
        val graphQlApiClient = GraphQlApiClient(
            graphQlApi,
            PublishableKey(publishableKey),
            deviceId,
            moshi,
            crashReportsProvider
        )

        val placesVisitsRepository = PlacesVisitsRepository(
            deviceId,
            graphQlApiClient,
            osUtilsProvider,
            crashReportsProvider,
            moshi
        )

        val placesVisitsInteractor = PlacesVisitsInteractor(
            placesVisitsRepository
        )

        val historyInteractorLegacy = HistoryInteractorImpl(
            historyRepository,
            GlobalScope
        )

        val historyInteractor = GraphQlHistoryInteractor(
            deviceId,
            graphQlApiClient,
            osUtilsProvider,
            crashReportsProvider,
            moshi,
            appScope.appCoroutineScope,
            appScope.stateMachineContext
        )

        val googlePlacesInteractor = GooglePlacesInteractorImpl(
            placesClient
        )

        val geotagsInteractor = GeotagsInteractor(
            hyperTrackService
        )

        val summaryInteractor = SummaryInteractor(historyInteractorLegacy)

        val userScope = UserScope(
            tripsInteractor,
            TripsUpdateTimerInteractor(tripsInteractor),
            placesInteractor,
            placesVisitsInteractor,
            googlePlacesInteractor,
            geotagsInteractor,
            historyInteractor,
            historyInteractorLegacy,
            summaryInteractor,
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
                crashReportsProvider,
                hyperTrackService,
                permissionsInteractor,
                accessTokenRepository,
                osUtilsProvider,
                deviceLocationProvider,
            ),
            deviceLocationProvider
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
                appScope,
                accessTokenRepository(MyApplication.context),
                getPermissionInteractor(),
                getOsUtilsProvider(MyApplication.context),
                appScope.crashReportsProvider,
                appScope.moshi,
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

    private fun createRemoteApi(
        baseUrl: String,
        moshi: Moshi,
        accessTokenRepository: BasicAuthAccessTokenRepository,
        crashReportsProvider: CrashReportsProvider
    ): ApiInterface {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor {
                        val response = it.proceed(it.request())
                        crashReportsProvider.log("${it.request().method} ${it.request().url.encodedPath} ${response.code}")
                        response
                    }
                    .authenticator(AccessTokenAuthenticator(accessTokenRepository))
                    .addInterceptor(AccessTokenInterceptor(accessTokenRepository))
                    .addInterceptor(UserAgentInterceptor())
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS).apply {
                        if (MyApplication.DEBUG_MODE) {
                            addInterceptor(HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            })
                        }
                    }.build()
            )
            .build()
            .create(ApiInterface::class.java)
    }

    private fun createGraphQlApi(baseUrl: String, moshi: Moshi): GraphQlApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(GraphQlApi::class.java)
    }

    private val placesClient: PlacesClient by lazy {
        Places.createClient(MyApplication.context)
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

    fun getPushReceiver(): PushReceiver {
        return PushReceiver(
            { getAccountRepo(MyApplication.context) },
            { getUserScope().tripsInteractor },
            appScope.notificationsInteractor,
            crashReportsProvider,
            getMoshi()
        )
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
    val geotagsInteractor: GeotagsInteractor,
    val historyInteractor: HistoryInteractor,
    val historyInteractorLegacy: HistoryInteractorImpl,
    val summaryInteractor: SummaryInteractor,
    val feedbackInteractor: FeedbackInteractor,
    val integrationsRepository: IntegrationsRepository,
    val hyperTrackService: HyperTrackService,
    val photoUploadQueueInteractor: PhotoUploadQueueInteractor,
    val apiClient: ApiClient,
    val userScopeViewModelFactory: UserScopeViewModelFactory,
    val deviceLocationProvider: DeviceLocationProvider
) {
    fun onDestroy() {
        tripsUpdateTimerInteractor.onDestroy()
    }
}

class AppScope(
    val appContext: Context,
    val driverRepository: DriverRepository,
    val deeplinkInteractor: DeeplinkInteractor,
    val notificationsInteractor: NotificationsInteractor,
    val loginInteractor: LoginInteractor,
    val crashReportsProvider: CrashReportsProvider,
    val osUtilsProvider: OsUtilsProvider,
    val dateTimeFormatter: DateTimeFormatter,
    val distanceFormatter: DistanceFormatter,
    val timeFormatter: TimeValueFormatter,
    val geofenceAddressDelegate: GeofenceAddressDelegate,
    val geofenceVisitDisplayDelegate: GeofenceVisitDisplayDelegate,
    val deviceStatusMarkerDisplayDelegate: DeviceStatusMarkerDisplayDelegate,
    val geotagDisplayDelegate: GeotagDisplayDelegate,
    val deeplinkProcessor: DeeplinkProcessor,
    val mapItemsFactory: HypertrackMapItemsFactory,
    val accountRepositoryProvider: Provider<AccountRepository>,
    val moshi: Moshi,
    val appCoroutineScope: CoroutineScope,
    val stateMachineContext: CoroutineContext
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

data class DeviceId(val value: String)
data class PublishableKey(val value: String)

const val BASE_URL = "https://live-app-backend.htprod.hypertrack.com/"
const val LIVE_API_URL_BASE = "https://live-api.htprod.hypertrack.com/"
const val AUTH_URL = LIVE_API_URL_BASE + "authenticate"
const val GRAPHQL_API_URL =
    "https://s6a3q7vbqzfalfhqi2vr32ugee.appsync-api.us-west-2.amazonaws.com/"
const val MAX_IMAGE_SIDE_LENGTH_PX = 1024

const val LIVE_ACCOUNT_URL_BASE = "https://live-account.htprod.hypertrack.com"
