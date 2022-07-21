package com.hypertrack.android.di

import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.interactors.FeedbackInteractor
import com.hypertrack.android.interactors.GeotagsInteractor
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.interactors.PhotoUploadQueueInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.interactors.trip.TripsUpdateTimerInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.use_case.handle_push.HandlePushUseCase
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.HyperTrackService

class UserScope(
    val appScope: AppScope,
    val appInteractor: AppInteractor,
    // interactors
    val tripsInteractor: TripsInteractor,
    val tripsUpdateTimerInteractor: TripsUpdateTimerInteractor,
    val placesInteractor: PlacesInteractor,
    val placesVisitsInteractor: PlacesVisitsInteractor,
    val googlePlacesInteractor: GooglePlacesInteractor,
    val geotagsInteractor: GeotagsInteractor,
    val feedbackInteractor: FeedbackInteractor,
    val photoUploadQueueInteractor: PhotoUploadQueueInteractor,
    val permissionsInteractor: PermissionsInteractor,
    // repositories
    val integrationsRepository: IntegrationsRepository,
    val placesRepository: PlacesRepository,
    val measurementUnitsRepository: MeasurementUnitsRepository,
    // other
    val hyperTrackService: HyperTrackService,
    val deviceId: DeviceId,
    val apiClient: ApiClient,
    val graphQlApiClient: GraphQlApiClient,
    val deviceLocationProvider: DeviceLocationProvider,
    val handlePushUseCase: HandlePushUseCase,
    val mapUiReducer: MapUiReducer,
    val mapUiEffectHandler: MapUiEffectHandler
) {

    val userScopeViewModelFactory = UserScopeViewModelFactory(
        appInteractor, appScope, this
    )

    fun onDestroy() {
        tripsUpdateTimerInteractor.onDestroy()
    }

    override fun toString(): String = javaClass.simpleName
}
