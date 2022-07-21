package com.hypertrack.android.ui.common.select_destination

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.utils.DeviceLocationProvider
import kotlinx.coroutines.flow.StateFlow

data class SelectDestinationViewModelDependencies(
    val userState: StateFlow<UserLoggedIn?>,
    val googlePlacesInteractor: GooglePlacesInteractor,
    val geocodingInteractor: GeocodingInteractor,
    val deviceLocationProvider: DeviceLocationProvider,
    val mapUiReducer: MapUiReducer,
    val mapUiEffectHandler: MapUiEffectHandler
)
