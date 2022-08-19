package com.hypertrack.android.use_case.app

import com.hypertrack.android.api.AccessTokenAuthenticator
import com.hypertrack.android.api.AccessTokenInterceptor
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.ApiInterface
import com.hypertrack.android.api.UserAgentInterceptor
import com.hypertrack.android.api.graphql.GraphQlApi
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.FeedbackInteractor
import com.hypertrack.android.interactors.GeotagsInteractor
import com.hypertrack.android.interactors.GooglePlacesInteractorImpl
import com.hypertrack.android.interactors.PermissionsInteractorImpl
import com.hypertrack.android.interactors.PhotoUploadQueueInteractorImpl
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.interactors.PlacesVisitsRepository
import com.hypertrack.android.interactors.trip.TripsInteractorImpl
import com.hypertrack.android.interactors.trip.TripsUpdateTimerInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.repository.IntegrationsRepositoryImpl
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.PlacesRepositoryImpl
import com.hypertrack.android.repository.TripsRepositoryImpl
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.use_case.app.AppCreationUseCase.Companion.LIVE_API_URL_BASE
import com.hypertrack.android.use_case.handle_push.HandlePushUseCase
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.FusedDeviceLocationProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.RetryParams
import com.hypertrack.sdk.HyperTrack
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

@Suppress("OPT_IN_USAGE")
class CreateUserScopeUseCase(
    private val appScope: AppScope,
    private val appInteractor: AppInteractor,
) {

    fun execute(
        hyperTrackSdk: HyperTrack,
        publishableKey: PublishableKey,
    ): Flow<UserScope> {
        return {
            createUserScope(
                appScope,
                appInteractor,
                hyperTrackSdk,
                publishableKey,
            )
        }.asFlow()
    }

    private fun createUserScope(
        appScope: AppScope,
        appInteractor: AppInteractor,
        hyperTrackSdk: HyperTrack,
        publishableKey: PublishableKey,
    ): UserScope {
        val crashReportsProvider = appScope.crashReportsProvider
        val osUtilsProvider = appScope.osUtilsProvider
        val moshi = appScope.moshi
        val deviceId = DeviceId(hyperTrackSdk.deviceID)
        val hyperTrackService = HyperTrackService(
            hyperTrackSdk,
            appScope.crashReportsProvider
        )
        val deviceLocationProvider = FusedDeviceLocationProvider(
            appInteractor,
            appScope.appCoroutineScope,
            appScope.appContext,
            hyperTrackService,
            crashReportsProvider,
        )
        val api = createRemoteApi(
            BASE_URL,
            deviceId,
            publishableKey,
            moshi,
            appScope.accessTokenRepository,
            crashReportsProvider
        )
        val apiClient = ApiClient(
            api,
            deviceId,
            moshi,
            crashReportsProvider
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
            appInteractor,
            placesRepository,
            integrationsRepository,
            osUtilsProvider,
            appScope.geofenceNameDelegate,
            appScope.appCoroutineScope
        )

        val photoUploadQueueInteractor = PhotoUploadQueueInteractorImpl(
            appScope.myPreferences,
            appScope.fileRepository,
            crashReportsProvider,
            appScope.imageDecoder,
            apiClient,
            scope,
            //todo to constants
            RetryParams(
                retryTimes = 3,
                initialDelay = 1000,
                factor = 10.0,
                maxDelay = 30 * 1000
            )
        )

        val tripsRepository = TripsRepositoryImpl(
            apiClient,
            appScope.myPreferences,
            appScope.appCoroutineScope,
            crashReportsProvider,
            appScope.orderAddressDelegate
        )

        val tripsInteractor = TripsInteractorImpl(
            appInteractor.appState,
            tripsRepository,
            apiClient,
            photoUploadQueueInteractor,
            appScope.imageDecoder,
            osUtilsProvider,
            crashReportsProvider,
            Dispatchers.IO,
            appScope.appCoroutineScope
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
            publishableKey,
            deviceId,
            moshi,
            crashReportsProvider
        )

        val placesVisitsRepository = PlacesVisitsRepository(
            deviceId,
            graphQlApiClient,
            appScope.graphQlGeofenceVisitAddressDelegate,
            crashReportsProvider,
            moshi
        )

        val placesVisitsInteractor = PlacesVisitsInteractor(
            placesVisitsRepository
        )

        val googlePlacesInteractor = GooglePlacesInteractorImpl(
            appScope.placesClient
        )

        val geotagsInteractor = GeotagsInteractor(
            hyperTrackService
        )

        val permissionsInteractor = PermissionsInteractorImpl(
            appScope.appContext,
            hyperTrackService
        )

        val measurementUnitsRepository = MeasurementUnitsRepository(
            appScope.preferencesRepository,
            crashReportsProvider
        )

        return UserScope(
            appScope,
            appInteractor,
            tripsInteractor,
            TripsUpdateTimerInteractor(tripsInteractor),
            placesInteractor,
            placesVisitsInteractor,
            googlePlacesInteractor,
            geotagsInteractor,
            feedbackInteractor,
            photoUploadQueueInteractor,
            permissionsInteractor,
            integrationsRepository,
            placesRepository,
            measurementUnitsRepository,
            hyperTrackService,
            deviceId,
            apiClient,
            graphQlApiClient,
            deviceLocationProvider,
            HandlePushUseCase(
                appScope.appContext,
                appScope.moshi,
                appScope.crashReportsProvider,
                appScope.resourceProvider,
                appScope.notificationUtil,
                appScope.dateTimeFormatter,
            ),
            MapUiReducer(),
            MapUiEffectHandler(appInteractor)
        )
    }

    private fun createRemoteApi(
        baseUrl: String,
        deviceId: DeviceId,
        publishableKey: PublishableKey,
        moshi: Moshi,
        accessTokenRepository: AccessTokenRepository,
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
                    .authenticator(
                        AccessTokenAuthenticator(
                            deviceId,
                            publishableKey,
                            appInteractor,
                            accessTokenRepository,
                            crashReportsProvider
                        )
                    )
                    .addInterceptor(AccessTokenInterceptor(accessTokenRepository))
                    .addInterceptor(UserAgentInterceptor())
                    .readTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                    .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS).apply {
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
                    //todo constants
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(GraphQlApi::class.java)
    }

    companion object {
        const val BASE_URL = "https://live-app-backend.htprod.hypertrack.com/"
        const val AUTH_URL = LIVE_API_URL_BASE + "authenticate"
        const val GRAPHQL_API_URL =
            "https://s6a3q7vbqzfalfhqi2vr32ugee.appsync-api.us-west-2.amazonaws.com/"

        const val CONNECTION_TIMEOUT = 30000L
    }

}
