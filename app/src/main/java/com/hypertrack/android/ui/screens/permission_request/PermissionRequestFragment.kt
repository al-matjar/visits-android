package com.hypertrack.android.ui.screens.permission_request

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_permission_request.*

class PermissionRequestFragment : ProgressDialogFragment(R.layout.fragment_permission_request) {

    private val vm: PermissionRequestViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        vm.showSkipButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { visible ->
            btnSkip.setGoneState(!visible)
        }

        vm.showPermissionsButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { show ->
            btnAllow.setGoneState(!show)
        }

        btnSkip.setOnClickListener { vm.handleAction(OnSkipClickedAction) }
        btnAllow.setOnClickListener { vm.handleAction(RequestPermissionsAction(mainActivity())) }

    }

    override fun onResume() {
        super.onResume()
        vm.handleAction(OnResumeAction(mainActivity()))
    }
}
