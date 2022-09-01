package com.hypertrack.android.ui.screens.add_integration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.AddIntegrationScreen
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.common.EndlessScrollListener
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragment
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_integration.*
import kotlinx.android.synthetic.main.fragment_add_place_info.toolbar
import kotlinx.android.synthetic.main.fragment_places.rvPlaces
import kotlinx.android.synthetic.main.fragment_select_destination.search
import kotlinx.coroutines.FlowPreview

@FlowPreview
class AddIntegrationFragment : BaseFragment<MainActivity>(R.layout.fragment_add_integration) {

    private val vm: AddIntegrationViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(AddIntegrationScreen))

        toolbar.title = ""
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        rvIntegrations.setLinearLayoutManager(requireContext())
        rvIntegrations.adapter = vm.adapter
//        rvIntegrations.addOnScrollListener(object : EndlessScrollListener(object : OnLoadMoreListener {
//            override fun onLoadMore(page: Int, totalItemsCount: Int) {
//                vm.handleAction(OnLoadMoreAction)
//            }
//        }) {
//            override val visibleThreshold = 1
//        })

        vm.viewState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { viewState ->
            lIntegrationsPlaceholder.setGoneState(!viewState.showPlaceholder)
            srlIntegrations.isRefreshing = viewState.showProgressbar
            rvIntegrations.setGoneState(!viewState.showList)
            bSearch.setGoneState(!viewState.showSearchButton)
            etSearch.setGoneState(!viewState.showSearchField)
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.integrationSelectedEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            it.consume { integration ->
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    AddPlaceInfoFragment.KEY_INTEGRATION,
                    integration
                )
                findNavController().popBackStack()
                /**
                 * showing and hiding the keyboard is disabled because of issues on some devices
                 * (the cause is unknown yet)
                 * when calling this method caused blocking keyboard state
                 * and it wasn't showed even on view click
                 */
//                Utils.hideKeyboard(requireActivity())
            }
        }

        etSearch.addTextChangedListener {
            vm.handleAction(OnQueryChangedAction(it.toString()))
        }

        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (Utils.isDoneAction(actionId, event)) {
                vm.handleAction(OnInitSearchAction)
                true
            } else false
        }

        srlIntegrations.setOnRefreshListener {
            vm.handleAction(OnRefreshAction)
        }

        bSearch.setOnClickListener {
            vm.handleAction(OnInitSearchAction)
        }

        vm.handleAction(InitAction)

        /**
         * showing and hiding the keyboard is disabled because of issues on some devices
         * (the cause is unknown yet)
         * when calling this method caused blocking keyboard state
         * and it wasn't showed even on view click
         */
//        Utils.showKeyboard(requireActivity(), etSearch)
    }
}
