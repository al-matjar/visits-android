package com.hypertrack.android.ui.screens.add_order

import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.map_state.MapUiEffectHandler
import com.hypertrack.android.ui.common.map_state.MapUiReducer
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModelDependencies
import com.hypertrack.android.utils.DeviceLocationProvider
import kotlinx.coroutines.flow.StateFlow


class AddOrderViewModel(
    private val tripId: String,
    baseDependencies: BaseViewModelDependencies,
    dependencies: SelectDestinationViewModelDependencies,
) : SelectDestinationViewModel(
    baseDependencies,
    dependencies
) {
    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddOrderFragmentDirections.actionAddOrderFragmentToAddOrderInfoFragment(
                destinationData, tripId
            )
        )
    }
}
