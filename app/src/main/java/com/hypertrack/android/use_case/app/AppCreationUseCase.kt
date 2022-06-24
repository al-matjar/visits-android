package com.hypertrack.android.use_case.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.libraries.places.api.Places
import com.hypertrack.android.api.GeometryJsonAdapter
import com.hypertrack.android.api.HistoryCoordinateJsonAdapter
import com.hypertrack.android.api.HistoryGeofenceMarker
import com.hypertrack.android.api.HistoryMarker
import com.hypertrack.android.api.HistoryStatusMarker
import com.hypertrack.android.api.HistoryTripMarker
import com.hypertrack.android.api.LiveAccountApi
import com.hypertrack.android.api.UserAgentInterceptor
import com.hypertrack.android.api.api_interface.AppBackendApi
import com.hypertrack.android.api.api_interface.AppBackendApi.Companion.APP_BACKEND_URL
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerActiveData
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerData
import com.hypertrack.android.api.graphql.models.GraphQlDeviceStatusMarkerInactiveData
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.FileRepositoryImpl
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.MyPreferences
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.repository.PublishableKeyRepository
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.ui.common.delegates.DateTimeRangeFormatterDelegate
import com.hypertrack.android.ui.common.delegates.DeviceStatusMarkerDisplayDelegate
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.GeofenceVisitAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.common.delegates.display.GeofenceVisitDisplayDelegate
import com.hypertrack.android.ui.common.delegates.display.GeotagDisplayDelegate
import com.hypertrack.android.ui.common.map.HypertrackMapItemsFactory
import com.hypertrack.android.use_case.sdk.NewTrackingState
import com.hypertrack.android.utils.BatteryLevelMonitor
import com.hypertrack.android.utils.CognitoAccountLoginProviderImpl
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.SimpleImageDecoder
import com.hypertrack.android.utils.CognitoExchangeTokenApi
import com.hypertrack.android.utils.exception.SimpleException
import com.hypertrack.android.utils.formatters.DateTimeFormatterImpl
import com.hypertrack.android.utils.formatters.LocalizedDistanceFormatter
import com.hypertrack.android.utils.formatters.TimeValueFormatterImpl
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.recipes.RuntimeJsonAdapterFactory
import com.squareup.moshi.recipes.ZonedDateTimeJsonAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.*
import java.util.concurrent.Executors

class AppCreationUseCase {

    fun execute(
        application: Application,
        crashReportsProvider: CrashReportsProvider,
        trackingStateListener: (NewTrackingState) -> Unit
    ): AppScope {
        buildNotificationChannels(application)
        //todo handle errors
        upgradeSecurityProvider(application, crashReportsProvider)

        Places.initialize(
            application,
            application.getString(R.string.google_places_api_key),
            Locale.getDefault()
        )

        val appScope = createAppScope(
            application,
            crashReportsProvider,
            trackingStateListener
        )

        BranchWrapper.init(application)

        appScope.batteryLevelMonitor.init(application)

        return appScope
    }

    private fun createAppScope(
        appContext: Context,
        crashReportsProvider: CrashReportsProvider,
        trackingStateListener: (NewTrackingState) -> Unit
    ): AppScope {
        val moshi = createMoshi()
        val osUtilsProvider = OsUtilsProvider(appContext, crashReportsProvider)
        val myPreferences = MyPreferences(appContext, moshi, crashReportsProvider)
        val preferencesRepository = PreferencesRepository(
            myPreferences,
            moshi,
        )
        val measurementUnitsRepository = MeasurementUnitsRepository(
            preferencesRepository,
            crashReportsProvider
        )
        val datetimeFormatter = DateTimeFormatterImpl()
        val distanceFormatter = LocalizedDistanceFormatter(
            osUtilsProvider,
            measurementUnitsRepository
        )
        val geocodingInteractor = GeocodingInteractor(
            appContext,
            crashReportsProvider
        )
        val timeFormatter = TimeValueFormatterImpl(osUtilsProvider)
        val geofenceVisitAddressDelegate = GeofenceVisitAddressDelegate(
            geocodingInteractor,
            osUtilsProvider
        )
        val geofenceAddressDelegate = GeofenceAddressDelegate(
            geocodingInteractor
        )
        val datetimeRangeFormatterDelegate = DateTimeRangeFormatterDelegate(
            osUtilsProvider,
            datetimeFormatter
        )
        val cognitoTokenExchangeApi = Retrofit.Builder()
            .baseUrl(LIVE_API_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CognitoExchangeTokenApi::class.java)
        val appBackendApi = Retrofit.Builder()
            .baseUrl(APP_BACKEND_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AppBackendApi::class.java)
        val liveAccountService = Retrofit.Builder()
            .baseUrl(LIVE_ACCOUNT_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LiveAccountApi::class.java)

        val accessTokenRepository = AccessTokenRepository(
            CreateUserScopeUseCase.AUTH_URL,
            moshi,
            preferencesRepository,
            crashReportsProvider,
            OkHttpClient.Builder().addInterceptor(UserAgentInterceptor()).build()
        )
        val publishableKeyRepository = PublishableKeyRepository(
            preferencesRepository,
            crashReportsProvider
        )
        val timeValueFormatter = TimeValueFormatterImpl(osUtilsProvider)
        val graphQlGeofenceVisitAddressDelegate = GraphQlGeofenceVisitAddressDelegate(
            geocodingInteractor,
            osUtilsProvider
        )
        return AppScope(
            MyApplication.context,
            geocodingInteractor,
            accessTokenRepository,
            publishableKeyRepository,
            UserRepository(preferencesRepository),
            preferencesRepository,
            FileRepositoryImpl(),
            measurementUnitsRepository,
            OrderAddressDelegate(geocodingInteractor, osUtilsProvider, datetimeFormatter),
            geofenceAddressDelegate,
            geofenceVisitAddressDelegate,
            graphQlGeofenceVisitAddressDelegate,
            GeofenceVisitDisplayDelegate(
                osUtilsProvider,
                distanceFormatter,
                timeValueFormatter,
                datetimeRangeFormatterDelegate,
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
            GeofenceNameDelegate(
                osUtilsProvider,
                datetimeFormatter
            ),
            BranchWrapper(crashReportsProvider),
            CognitoAccountLoginProviderImpl(appContext),
            appBackendApi,
            cognitoTokenExchangeApi,
            liveAccountService,
            myPreferences,
            crashReportsProvider,
            osUtilsProvider,
            osUtilsProvider,
            datetimeFormatter,
            distanceFormatter,
            timeFormatter,
            Places.createClient(appContext),
            SimpleImageDecoder(),
            HypertrackMapItemsFactory(osUtilsProvider),
            BatteryLevelMonitor(crashReportsProvider),
            NotificationUtil,
            moshi,
            trackingStateListener,
            CoroutineScope(SupervisorJob()),
            Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
        )
    }

    private fun buildNotificationChannels(appContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                appContext.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                AppInteractor.CHANNEL_ID,
                appContext.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = appContext.getString(R.string.channel_description)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(
                AppInteractor.IMPORTANT_CHANNEL_ID,
                appContext.getString(R.string.notification_important_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    appContext.getString(R.string.notification_important_channel_description)
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
                context,
                object : ProviderInstaller.ProviderInstallListener {
                    override fun onProviderInstalled() {
                        crashReportsProvider.log("Security Provider installed")
                    }

                    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                        try {
                            GoogleApiAvailability.getInstance()
                                .showErrorNotification(context, errorCode)
                            crashReportsProvider.logException(
                                SimpleException("Security provider installation failed, error code: $errorCode")
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
        const val LIVE_API_URL_BASE = "https://live-api.htprod.hypertrack.com/"
        const val LIVE_ACCOUNT_URL_BASE = "https://live-account.htprod.hypertrack.com"

        fun createMoshi(): Moshi {
            return Moshi.Builder()
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
                    RuntimeJsonAdapterFactory(
                        GraphQlDeviceStatusMarkerData::class.java,
                        "__typename"
                    )
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
        }
    }

}
