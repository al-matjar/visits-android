package com.hypertrack.android.ui.screens.add_order_info

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.interactors.AddOrderError
import com.hypertrack.android.interactors.AddOrderSuccess
import com.hypertrack.android.interactors.GeocodingInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.delegates.address.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.add_order.AddOrderFragmentDirections
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.TripCreationScope
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddOrderInfoViewModel(
    private val params: AddOrderParams,
    baseDependencies: BaseViewModelDependencies,
    private val tripsInteractor: TripsInteractor,
    private val geocodingInteractor: GeocodingInteractor,
) : BaseViewModel(baseDependencies) {

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)

    val destinationData = params.destinationData

    val address = MutableLiveData<String?>()

    val enableConfirmButton = MutableLiveData<Boolean>(false)

    init {
        viewModelScope.launch {
            loadAddress().let {
                address.postValue(it)
                enableConfirmButton.postValue(true)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationData.latLng, 13f))
        googleMap.addMarker(MarkerOptions().position(destinationData.latLng))
    }

    fun onConfirmClicked(address: String) {
        if (enableConfirmButton.value == true) {
            when (params) {
                is AddOrderToTripParams -> {
                    viewModelScope.launch {
                        loadingState.postValue(true)
                        val res = tripsInteractor.addOrderToTrip(
                            tripId = params.tripId,
                            destinationData.latLng,
                            address
                        )
                        when (res) {
                            is AddOrderSuccess -> {
                                destination.postValue(
                                    NavGraphDirections.actionGlobalVisitManagementFragment(Tab.CURRENT_TRIP)
                                )
                            }
                            is AddOrderError -> {
                                errorHandler.postException(res.e)
                            }
                        }
                        loadingState.postValue(false)
                    }
                }
                is NewTripParams -> {
                    MyApplication.injector.tripCreationScope = TripCreationScope(destinationData)
                    destination.postValue(
                        AddOrderFragmentDirections
                            .actionGlobalVisitManagementFragment(Tab.CURRENT_TRIP)
                    )
                }
            }
        }
    }

    fun onAddressChanged(address: String) {
        if (this.address.value != address) {
            this.address.postValue(address)
        }
    }

    private suspend fun loadAddress(): String? {
        return destinationData.address
            ?: withContext(Dispatchers.IO) {
                geocodingInteractor.getPlaceFromCoordinates(destinationData.latLng)?.let {
                    //todo set edittext hint with partial address
                    addressDelegate.strictAddress(it)
                }
            }
    }

}

sealed class AddOrderParams(val destinationData: DestinationData)
class NewTripParams(destinationData: DestinationData) : AddOrderParams(destinationData)
class AddOrderToTripParams(
    destinationData: DestinationData,
    val tripId: String
) : AddOrderParams(destinationData)


