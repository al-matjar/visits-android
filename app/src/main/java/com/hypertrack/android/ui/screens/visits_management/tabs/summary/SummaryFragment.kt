package com.hypertrack.android.ui.screens.visits_management.tabs.summary

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.setLinearLayoutManager
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_tab_summary.*

class SummaryFragment : ProgressDialogFragment(R.layout.fragment_tab_summary) {

    private val adapter = SummaryItemsAdapter()

    private val vm: SummaryViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvSummary.setLinearLayoutManager(requireContext())
        rvSummary.adapter = adapter

        displayLoadingState(true)

        vm.summary.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { summary ->
            adapter.updateItems(summary)
            displayLoadingState(false)
        }

        srlSummary.setOnRefreshListener {
            vm.refreshSummary()
        }
    }

    private fun displayLoadingState(isLoading: Boolean) {
        srlSummary.isRefreshing = isLoading
    }

    companion object {
        fun newInstance() = SummaryFragment()
    }

}
