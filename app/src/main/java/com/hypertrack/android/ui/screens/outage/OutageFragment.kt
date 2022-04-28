package com.hypertrack.android.ui.screens.outage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.toView
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_outage.toolbar
import kotlinx.android.synthetic.main.fragment_outage.tvOutageDescription
import kotlinx.android.synthetic.main.fragment_outage.tvOutageTitle

class OutageFragment : BaseFragment<MainActivity>(R.layout.fragment_outage) {

    private val args: OutageFragmentArgs by navArgs()
    private val vm: OutageViewModel by viewModels {
        Injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = ""
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        vm.init(args.outageNotification)

        vm.viewState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            it.title.toView(tvOutageTitle)
            it.description.toView(tvOutageDescription)
        }
    }


}
