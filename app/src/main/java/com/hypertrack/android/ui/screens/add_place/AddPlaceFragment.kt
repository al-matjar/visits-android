package com.hypertrack.android.ui.screens.add_place

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.AddPlaceScreen
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*
import kotlinx.coroutines.FlowPreview

@FlowPreview
open class AddPlaceFragment : SelectDestinationFragment() {

    override val vm: AddPlaceViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.add_place)

        vm.adjacentGeofenceDialog.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            it.consume {
                vm.createConfirmationDialog(requireContext(), it).show()
            }
        }
    }

    override fun registerScreen() {
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(AddPlaceScreen))
    }
}
