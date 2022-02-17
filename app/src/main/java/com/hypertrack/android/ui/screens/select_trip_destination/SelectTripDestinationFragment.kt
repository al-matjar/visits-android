package com.hypertrack.android.ui.screens.select_trip_destination

import androidx.fragment.app.viewModels
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment

class SelectTripDestinationFragment : SelectDestinationFragment() {

    override val vm: SelectTripDestinationViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

}
