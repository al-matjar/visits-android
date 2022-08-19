package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.app.AlertDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.delegates.map_view.GoogleMapViewDelegate
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_current_trip.*
import kotlinx.android.synthetic.main.inflate_current_trip.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.coroutines.FlowPreview

class CurrentTripFragment : ProgressDialogFragment(R.layout.fragment_current_trip) {

    override val delegates = listOf(
        GoogleMapViewDelegate(
            R.id.map,
            this,
            Injector.provideAppInteractor()
        ) {
            vm.handleAction(InitMapAction(requireContext(), it))
        }
    )

    private val vm: CurrentTripViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    private lateinit var bottomHolderSheetBehavior: BottomSheetBehavior<*>

    private val ordersAdapter by lazy { vm.createOrdersAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.handleAction(OnViewCreatedAction)

        bottomHolderSheetBehavior = BottomSheetBehavior.from(bottom_holder)
        bottom_holder.show()
        val bottomHolder = bottom_holder
        recycler_view.setLinearLayoutManager(requireContext())
        recycler_view.adapter = ordersAdapter.apply {
            onItemClickListener = {
                vm.handleAction(OnOrderClickAction(it.id))
            }
        }

        bottomHolder.setOnClickListener {
            if (bottomHolderSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomHolderSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                vm.handleAction(OnTripFocusedAction)
            } else {
                bottomHolderSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        vm.viewState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { viewState ->
            location_button.setGoneState(viewState.userLocation == null)
            whereAreYouGoing.setGoneState(!viewState.showWhereAreYouGoingButton)
            viewState.tripData.let { tripData ->
                lTrip.setGoneState(tripData == null)
                tripData?.let { displayTrip(it) }
            }
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            whereAreYouGoing.setGoneState(it)
            progress.setGoneState(!it)
            if (it) {
                loader.playAnimation()
            } else {
                loader.cancelAnimation()
            }
        }

        whereAreYouGoing.setOnClickListener {
            vm.handleAction(OnWhereAreYouGoingClickAction)
        }

        shareButton.setOnClickListener {
            vm.handleAction(OnShareTripClickAction)
        }

        bAddOrder.setOnClickListener {
            vm.handleAction(OnAddOrderClickAction)
        }

        bCompleteTrip.setOnClickListener {
            createCompleteTripDialog().show()
        }

        location_button.setOnClickListener {
            vm.handleAction(OnMyLocationClickAction)
        }

        if (MyApplication.DEBUG_MODE) {
            trip_to.setOnClickListener {
                vm.handleAction(OnCompleteClickAction)
            }
        }
    }

    override fun onPause() {
        vm.handleAction(OnPauseAction)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        vm.handleAction(OnResumeAction)
    }

    private fun displayTrip(trip: TripData) {
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
                    it.goneIfNull(order.awayText)
                }
                order.awayText?.toView(destination_away)
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
                vm.handleAction(OnCompleteClickAction)
            }
            .setNegativeButton(R.string.no) { _, _ ->
            }
            .create()
    }

    companion object {
        const val KEY_DESTINATION = "destination"
    }

}
