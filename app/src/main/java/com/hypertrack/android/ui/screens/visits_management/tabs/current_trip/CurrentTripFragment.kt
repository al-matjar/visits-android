package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_current_trip.*
import kotlinx.android.synthetic.main.inflate_current_trip.*
import kotlinx.android.synthetic.main.progress_bar.*

class CurrentTripFragment : ProgressDialogFragment(R.layout.fragment_current_trip) {

    private lateinit var bottomHolderSheetBehavior: BottomSheetBehavior<*>

    private val vm: CurrentTripViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    private val ordersAdapter by lazy { vm.createOrdersAdapter() }

    private lateinit var map: GoogleMap
    private val mapStyleActive by lazy {
        MapStyleOptions.loadRawResourceStyle(
            requireContext(),
            R.raw.style_map
        )
    }
    private val mapStyleInactive by lazy {
        MapStyleOptions.loadRawResourceStyle(
            requireContext(),
            R.raw.style_map_silver
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<DestinationData>(KEY_DESTINATION)
            ?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    vm.onDestinationResult(it)
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.set(KEY_DESTINATION, null)
                }
            }

        //check if there is intent to create trip
        MyApplication.injector.tripCreationScope?.let {
            MyApplication.injector.tripCreationScope = null
            vm.onDestinationResult(it.destinationData)
        }

        vm.onViewCreated()

        (childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment?)?.getMapAsync {
            map = it
            vm.onMapReady(requireContext(), it)
        }

        bottomHolderSheetBehavior = BottomSheetBehavior.from(bottom_holder)
        bottom_holder.show()
        val bottomHolder = bottom_holder
        recycler_view.setLinearLayoutManager(requireContext())
        recycler_view.adapter = ordersAdapter.apply {
            onItemClickListener = {
                vm.onOrderClick(it.id)
            }
        }

        bottomHolder.setOnClickListener {
            if (bottomHolderSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomHolderSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            } else {
                bottomHolderSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        vm.userLocation.observe(viewLifecycleOwner, {
            location_button.setGoneState(it == null)
        })

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        vm.tripData.observe(viewLifecycleOwner, {
            lTrip.setGoneState(it == null)
            it?.let { displayTrip(it) }
            googleMap.setPadding(
                0, 0, 0,
                if (it != null) {
                    requireContext().resources
                        .getDimension(R.dimen.current_trip_bottom_bar_start_height).toInt()
                } else 0
            )
        })

        vm.showWhereAreYouGoing.observe(viewLifecycleOwner, {
            whereAreYouGoing.setGoneState(!it)
        })

        vm.errorHandler.errorText.observe(viewLifecycleOwner, {
            it.consume {
                SnackbarUtil.showErrorSnackbar(view, it)
            }
        })

        vm.loadingState.observe(viewLifecycleOwner, {
            whereAreYouGoing.setGoneState(it)
            progress.setGoneState(!it)
            if (it) {
                loader.playAnimation()
            } else {
                loader.cancelAnimation()
            }
        })

        vm.mapActiveState.observe(viewLifecycleOwner, {
            it?.let {
                //todo workaround for a bug on some devices
                if (this::map.isInitialized) {
                    map.setMapStyle(
                        if (it) {
                            mapStyleActive
                        } else {
                            mapStyleInactive
                        }
                    )
                }
            }
        })

        whereAreYouGoing.setOnClickListener {
            vm.onWhereAreYouGoingClick()
        }

        shareButton.setOnClickListener {
            vm.onShareTripClick()
        }

        bAddOrder.setOnClickListener {
            vm.onAddOrderClick()
        }

        bCompleteTrip.setOnClickListener {
            createCompleteTripDialog().show()
        }

        location_button.setOnClickListener {
            vm.onMyLocationClick()
        }

        if (MyApplication.DEBUG_MODE) {
            trip_to.setOnClickListener {
                vm.onCompleteClick()
            }
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

    private fun displayTrip(trip: CurrentTripViewModel.TripData) {
        bAddOrder.setGoneState(trip.isLegacy)

        trip.nextOrder.let { order ->
            bCompleteTrip.setGoneState(order != null)
            shareButton.setGoneState(order == null)
            listOf(
                shareButton,
                destination_icon,
                destination_address,
                destination_arrival,
                destination_arrival_title,
                destination_away,
                destination_away_title,
            ).forEach { it.setGoneState(order == null) }

            if (order != null) {
                order.address.toView(destination_address)
                order.etaString.toView(destination_arrival)
                destination_arrival.setTextSize(
                    TypedValue.COMPLEX_UNIT_DIP,
                    if (order.etaAvailable) 18f else 14f
                )
                listOf(
                    destination_arrival_title,
                    destination_away,
                    destination_away_title
                ).forEach {
                    it.setGoneState(!order.etaAvailable)
                }
                listOf(destination_away, destination_away_title).forEach {
                    it.setGoneState(order.awayText == null)
                }
                order.awayText.toView(destination_away)
            }
        }

        trip.ongoingOrders.let { orders ->
            recycler_view.setGoneState(orders.isEmpty())
            trip.ongoingOrderText.toView(trips_count)
            ordersAdapter.updateItems(orders)
        }
    }

    private fun createCompleteTripDialog(): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.current_trip_complete_confirmation.stringFromResource()
            )
            .setPositiveButton(R.string.yes) { dialog, which ->
                vm.onCompleteClick()
            }
            .setNegativeButton(R.string.no) { _, _ ->
            }
            .create()
    }

    companion object {
        const val KEY_DESTINATION = "destination"
    }

}