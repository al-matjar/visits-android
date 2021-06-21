package com.hypertrack.android.ui.screens.add_order_info

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_place_info.*
import kotlinx.android.synthetic.main.fragment_add_place_info.confirm
import kotlinx.android.synthetic.main.fragment_add_place_info.toolbar
import kotlinx.android.synthetic.main.inflate_integration.*

class AddOrderInfoFragment : ProgressDialogFragment(R.layout.fragment_add_order_info) {

    private val args: AddOrderInfoFragmentArgs by navArgs()
    private val vm: AddOrderInfoViewModel by viewModels {
        MyApplication.injector.provideParamVmFactory(
            AddOrderInfoViewModel.Params(
                args.tripId,
                args.destinationData
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.add_order)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mainActivity().supportActionBar!!.setHomeButtonEnabled(true)

        (childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?)?.getMapAsync {
            vm.onMapReady(it)
        }

        val listener = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                vm.onAddressChanged(text)
            }
        }
        etAddress.addTextChangedListener(listener)

        vm.address.observe(viewLifecycleOwner, {
            etAddress.silentUpdate(listener, it)
        })

        vm.error.observe(viewLifecycleOwner, {
            SnackbarUtil.showErrorSnackbar(view, it)
        })

        vm.loadingStateBase.observe(viewLifecycleOwner, {
            if (it) showProgress() else dismissProgress()
        })

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        vm.enableConfirmButton.observe(viewLifecycleOwner, { it ->
            confirm.isSelected = it
        })

        confirm.setOnClickListener {
            vm.onConfirmClicked(
                address = etAddress.textString(),
            )
        }

    }

}