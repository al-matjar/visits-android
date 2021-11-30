package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.Injector
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_places.*

class PlacesFragment : ProgressDialogFragment(R.layout.fragment_places) {

    private var state = State.PLACES
    private val vm: PlacesViewModel by viewModels { Injector.provideUserScopeViewModelFactory() }
    private val visitsVm: PlacesVisitsViewModel by viewModels { Injector.provideUserScopeViewModelFactory() }

    private lateinit var adapter: PlacesAdapter
    private lateinit var visitsAdapter: AllPlacesVisitsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPlaces()
        initVisits()

        vm.loadingState.observe(viewLifecycleOwner, {
            srlPlaces.isRefreshing = (vm.loadingState.value == true
                    || visitsVm.loadingState.value == true)
                    && when (state) {
                State.PLACES -> adapter.itemCount == 0
                State.VISITS -> visitsAdapter.itemCount == 0
            }
            paginationProgressbar.setGoneState(!it || adapter.itemCount == 0)
        })

        visitsVm.loadingState.observe(viewLifecycleOwner, {
            srlPlaces.isRefreshing = (vm.loadingState.value == true
                    || visitsVm.loadingState.value == true)
                    && when (state) {
                State.PLACES -> adapter.itemCount == 0
                State.VISITS -> visitsAdapter.itemCount == 0
            }
            paginationProgressbar.setGoneState(!it || adapter.itemCount == 0)
        })

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        visitsVm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        vm.errorHandler.errorText.observe(viewLifecycleOwner, {
            SnackbarUtil.showErrorSnackbar(view, it)
        })

        visitsVm.errorHandler.errorText.observe(viewLifecycleOwner, {
            SnackbarUtil.showErrorSnackbar(view, it)
        })

        srlPlaces.setOnRefreshListener {
            vm.refresh()
            visitsVm.refresh()
        }

        fbAddPlace.setOnClickListener {
            vm.onAddPlaceClicked()
        }

        bPlaces.setOnClickListener {
            displayState(State.PLACES)
        }

        bVisits.setOnClickListener {
            displayState(State.VISITS)
        }

        vm.init()
        visitsVm.init()
        displayState(State.PLACES)
    }

    private fun displayState(state: State) {
        this.state = state
        when (state) {
            State.PLACES -> {
                bPlaces.isSelected = true
                bPlaces.setTypeface(null, Typeface.BOLD)
                bVisits.isSelected = false
                bVisits.setTypeface(null, Typeface.NORMAL)
                lPlaces.setGoneState(false)
                lVisits.setGoneState(true)
            }
            State.VISITS -> {
                bPlaces.isSelected = false
                bPlaces.setTypeface(null, Typeface.NORMAL)
                bVisits.isSelected = true
                bVisits.setTypeface(null, Typeface.BOLD)
                lPlaces.setGoneState(true)
                lVisits.setGoneState(false)
            }
        }
    }

    private fun initPlaces() {
        rvPlaces.setLinearLayoutManager(requireContext())
        adapter = vm.createPlacesAdapter()
        rvPlaces.adapter = adapter
        adapter.onItemClickListener = {
            vm.onPlaceClick(it)
        }
        rvPlaces.addOnScrollListener(object : EndlessScrollListener(object : OnLoadMoreListener {
            override fun onLoadMore(page: Int, totalItemsCount: Int) {
                vm.onLoadMore()
            }
        }) {
            override val visibleThreshold = 1
        })

        vm.placesPage.observe(viewLifecycleOwner, {
            if (it != null) {
                it.consume {
//                    Log.v("hypertrack-verbose", "-- page ${it.map { it.geofence.name }}")
                    adapter.addItemsAndUpdate(it)
                    lPlacesPlaceholder.setGoneState(adapter.itemCount != 0)
                    rvPlaces.setGoneState(adapter.itemCount == 0)
                }
            } else {
                adapter.updateItems(listOf())
                lPlacesPlaceholder.hide()
                rvPlaces.show()
            }
        })
    }

    private fun initVisits() {
        rvVisits.setLinearLayoutManager(requireContext())
        visitsAdapter = visitsVm.createVisitsAdapter()
        rvVisits.adapter = visitsAdapter

        visitsVm.visitsStats.observe(viewLifecycleOwner, {
            visitsAdapter.updateItems(it)
            lVisitsPlaceholder.setGoneState(it.isNotEmpty())
            rvVisits.setGoneState(it.isEmpty())
        })
    }

    companion object {
        fun getInstance() = PlacesFragment()
    }

    enum class State {
        PLACES, VISITS
    }
}