package com.hypertrack.android.ui.screens.add_order

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment
import kotlinx.coroutines.FlowPreview

@FlowPreview
open class AddOrderFragment : SelectDestinationFragment() {

    private val args: AddOrderFragmentArgs by navArgs()

    protected override val vm: AddOrderViewModel by viewModels {
        Injector.provideUserScopeParamViewModelFactory(args.tripId)
    }

}
