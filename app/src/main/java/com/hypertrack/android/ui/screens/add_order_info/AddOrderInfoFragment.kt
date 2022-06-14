package com.hypertrack.android.ui.screens.add_order_info

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.SimpleTextWatcher
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.silentUpdate
import com.hypertrack.android.ui.common.util.textString
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_place_info.*
import kotlinx.android.synthetic.main.fragment_add_place_info.confirm
import kotlinx.android.synthetic.main.fragment_add_place_info.toolbar

class AddOrderInfoFragment : ProgressDialogFragment(R.layout.fragment_add_order_info) {

    private val args: AddOrderInfoFragmentArgs by navArgs()
    private val vm: AddOrderInfoViewModel by viewModels {
        Injector.provideUserScopeParamViewModelFactory(
            args.tripId.let { tripId ->
                if (tripId == null) {
                    NewTripParams(args.destinationData)
                } else {
                    AddOrderToTripParams(args.destinationData, tripId)
                }
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.add_order)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        (childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment).getMapAsync {
            vm.onMapReady(it)
        }

        val listener = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                vm.onAddressChanged(text)
            }
        }
        etAddress.addTextChangedListener(listener)

        vm.address.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            etAddress.silentUpdate(listener, it)
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            if (it) showProgress() else dismissProgress()
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        vm.enableConfirmButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { it ->
            confirm.isSelected = it
        }

        confirm.setOnClickListener {
            vm.onConfirmClicked(
                address = etAddress.textString(),
            )
        }
    }
}
