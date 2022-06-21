package com.hypertrack.android.ui.screens.visits_management.tabs.profile

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.adapters.KeyValueAdapter
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.setLinearLayoutManager
import com.hypertrack.android.ui.common.util.show
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : BaseFragment<MainActivity>(R.layout.fragment_profile) {

    private val adapter = KeyValueAdapter(showCopyButton = true)

    private val vm: ProfileViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spMeasurementUnits.adapter = vm.createMeasurementUnitsAdapter(requireActivity())
        spMeasurementUnits.setSelection(vm.getInitialMeasurementUnitItemIndex())
        spMeasurementUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                index: Int,
                l: Long
            ) {
                vm.onMeasurementUnitItemSelected(index)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        rvProfile.setLinearLayoutManager(requireContext())
        rvProfile.adapter = adapter
        adapter.onCopyClickListener = {
            vm.onCopyItemClick(it)
        }

        vm.profile.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            adapter.updateItems(it)
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        bReportAnIssue.setOnClickListener {
            vm.onReportAnIssueClick()
        }

        bOpenDontkillmyapp.setOnClickListener {
            vm.onOpenDontkillmyappClicked(requireActivity())
        }

        if (MyApplication.DEBUG_MODE) {
            bShowInDashboard.show()
            bShowInDashboard.setOnClickListener {
                vm.onShowOnDashboardClick(requireActivity())
            }
        }
    }

}
