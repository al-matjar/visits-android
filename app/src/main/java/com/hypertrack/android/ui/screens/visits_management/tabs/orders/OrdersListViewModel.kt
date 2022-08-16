package com.hypertrack.android.ui.screens.visits_management.tabs.orders

import androidx.lifecycle.*
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.interactors.trip.TripsUpdateTimerInteractor
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.Trip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.common.delegates.address.OrderAddressDelegate
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import kotlinx.coroutines.launch

@Suppress("IfThenToElvis")
class OrdersListViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val tripsInteractor: TripsInteractor,
    private val tripsUpdateTimerInteractor: TripsUpdateTimerInteractor,
    private val dateTimeFormatter: DateTimeFormatter,
    private val addressDelegate: OrderAddressDelegate,
) : BaseViewModel(baseDependencies) {

    val trip: LiveData<Trip?> = tripsInteractor.currentTrip

    val metadata: LiveData<List<KeyValueItem>> =
        Transformations.map(tripsInteractor.currentTrip) { trip ->
            if (trip != null) {
                trip.metadata
                    .filter { (key, _) -> !key.startsWith("ht_") }
                    .toList().map { KeyValueItem(it.first, it.second) }.toMutableList()
                    .apply {
                        if (MyApplication.DEBUG_MODE) {
                            add(KeyValueItem("trip_id (debug)", trip.id))
                        }
                    }
            } else {
                listOf()
            }
        }

    val orders: LiveData<List<Order>> =
        Transformations.map(tripsInteractor.currentTrip) { trip ->
            if (trip != null) {
                mutableListOf<Order>().apply {
                    addAll(trip.orders.filter { it.status == OrderStatus.ONGOING })
                    addAll(trip.orders.filter { it.status != OrderStatus.ONGOING })
                }
            } else {
                listOf()
            }
        }

    init {
        tripsInteractor.errorFlow.asLiveData().observeManaged {
            showExceptionMessageAndReport(it)
        }
        onRefresh()
    }

    fun onRefresh() {
        viewModelScope.launch {
            loadingState.postValue(true)
            tripsInteractor.refreshTrips()
            loadingState.postValue(false)
        }
    }

    fun onOrderClick(orderId: String) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToOrderDetailsFragment(
                orderId
            )
        )
    }

    fun onCopyClick(it: String) {
        osUtilsProvider.copyToClipboard(it)
    }

    fun onResume() {
        tripsUpdateTimerInteractor.registerObserver(this.javaClass.simpleName)
    }

    fun onPause() {
        tripsUpdateTimerInteractor.unregisterObserver(this.javaClass.simpleName)
    }

    fun createAdapter(): OrdersAdapter {
        return OrdersAdapter(
            dateTimeFormatter,
            addressDelegate
        )
    }

}
