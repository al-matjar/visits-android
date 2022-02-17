package com.hypertrack.android.ui.screens.visits_management.tabs.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.adapters.KeyValueAdapter
import com.hypertrack.android.ui.common.util.SnackbarUtil
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.setLinearLayoutManager
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_orders.*

class OrdersFragment : BaseFragment<MainActivity>(R.layout.fragment_orders) {

    private val vm: OrdersListViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    private val keyValueAdapter = KeyValueAdapter(showCopyButton = true)

    private val ordersAdapter by lazy { vm.createAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTripMetadata.setLinearLayoutManager(requireContext())
        rvTripMetadata.adapter = keyValueAdapter
        keyValueAdapter.onCopyClickListener = {
            vm.onCopyClick(it)
        }

        rvOrders.setLinearLayoutManager(requireContext())
        rvOrders.adapter = ordersAdapter

        ordersAdapter.onItemClickListener = {
            vm.onOrderClick(it.id)
        }

        vm.loadingState.observe(viewLifecycleOwner, {
            refreshLayout.isRefreshing = it
        })

        vm.metadata.observe(viewLifecycleOwner, {
            keyValueAdapter.updateItems(it)
        })

        vm.orders.observe(viewLifecycleOwner, {
            ordersAdapter.updateItems(it)
        })

        vm.trip.observe(viewLifecycleOwner, {
            lTrip.setGoneState(it == null)
            tvNoTripAssigned.setGoneState(it != null)
        })

        vm.error.observe(viewLifecycleOwner, { consumable ->
            consumable.consume {
                SnackbarUtil.showErrorSnackbar(view, it.message)
            }
        })

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        refreshLayout.setOnRefreshListener {
            vm.onRefresh()
        }
    }

    override fun onPause() {
        vm.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    fun refresh() {
        if (isAdded) {
            vm.onRefresh()
        }
    }

    companion object {
        fun newInstance() = OrdersFragment()
    }

}
