package com.hypertrack.android.ui.screens.select_trip_destination

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.utils.DeviceLocationProvider

class SelectTripDestinationViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val geocodingInteractor: GeocodingInteractor,
    private val deviceLocationProvider: DeviceLocationProvider,
) : SelectDestinationViewModel(
    baseDependencies,
    placesInteractor,
    googlePlacesInteractor,
    geocodingInteractor,
    deviceLocationProvider,
) {
    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            SelectTripDestinationFragmentDirections
                .actionSelectTripDestinationFragmentToAddOrderInfoFragment(
                    destinationData,
                    tripId = null
                )
        )
    }
}
