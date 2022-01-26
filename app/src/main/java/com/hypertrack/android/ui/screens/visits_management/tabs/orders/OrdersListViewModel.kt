package com.hypertrack.android.ui.screens.visits_management.tabs.orders

import androidx.lifecycle.*
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.interactors.TripsUpdateTimerInteractor
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
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
) : BaseViewModel(baseDependencies) {

    private val addressDelegate = OrderAddressDelegate(osUtilsProvider, dateTimeFormatter)

    val error = tripsInteractor.errorFlow.asLiveData()

    val trip: LiveData<LocalTrip?> = tripsInteractor.currentTrip

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

    val orders: LiveData<List<LocalOrder>> =
        Transformations.map(tripsInteractor.currentTrip) { trip ->
            if (trip != null) {
                mutableListOf<LocalOrder>().apply {
                    addAll(trip.orders.filter { it.status == OrderStatus.ONGOING })
                    addAll(trip.orders.filter { it.status != OrderStatus.ONGOING })
                }
            } else {
                listOf()
            }
        }

    init {
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
