package com.hypertrack.android.ui.screens.visits_management

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.TabsScreen
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.SimplePageChangedListener
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryFragment
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.adapter_place_item.name
import kotlinx.android.synthetic.main.fragment_visits_management.*

class VisitsManagementFragment : ProgressDialogFragment(R.layout.fragment_visits_management) {

    val args: VisitsManagementFragmentArgs by navArgs()

    private val ordersFragment = OrdersFragment.newInstance()
    private val historyFragment = HistoryFragment()
    private val tabsMap = mapOf(
        Tab.CURRENT_TRIP to CurrentTripFragment(),
        Tab.HISTORY to historyFragment,
        Tab.ORDERS to ordersFragment,
        Tab.PLACES to PlacesFragment.getInstance(),
        Tab.SUMMARY to SummaryFragment.newInstance(),
        Tab.PROFILE to ProfileFragment()
    )
    private val tabs = Injector.provideTabs()

    private val vm: VisitsManagementViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(TabsScreen))

        initViewPager()

        vm.trackingIndicatorState.observeWithErrorHandling(
            viewLifecycleOwner,
            vm::onError
        ) { state ->
            tvTrackerStatus.setBackgroundColor(requireContext().getColor(state.color))
            tvTrackerStatus.setText(state.statusMessageResource)
            swClockIn.setStateWithoutTriggeringListener(state.isTracking)
            tvClockHint.setText(state.trackingMessageResource)
        }

        vm.showProgressbar.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { show ->
            if (show) showProgress() else dismissProgress()
        }

        swClockIn.setOnCheckedChangeListener { view, isChecked ->
            vm.handleAction(TrackingSwitchClickedAction(isChecked))
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(
            viewLifecycleOwner,
            vm::onError
        ) { error ->
            SnackBarUtil.showErrorSnackBar(view, error)
        }

        vm.destination.observeWithErrorHandling(
            viewLifecycleOwner,
            vm::onError
        ) {
            findNavController().navigate(it)
        }

        vm.handleAction(OnViewCreatedAction)

        args.tab?.let { tab ->
            viewpager.currentItem = tabs.indexOf(args.tab)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshOrders()
    }

    override fun onBackPressed(): Boolean {
        return if (super.onBackPressed()) {
            true
        } else {
            if (viewpager?.currentItem == Tab.HISTORY.ordinal) {
                historyFragment.onBackPressed()
            } else {
                false
            }
        }
    }

    fun refreshOrders() {
        ordersFragment.refresh()
    }

    private fun initViewPager() {
        viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getCount(): Int = tabs.size

            override fun getItem(position: Int): Fragment {
                val fragment = tabsMap.getValue(tabs[position])
                return fragment
            }
        }

        viewpager.addOnPageChangeListener(object : SimplePageChangedListener() {
            override fun onPageSelected(position: Int) {
                tabs[position].let {
                    Injector.crashReportsProvider.log(
                        "Tab selected ${it.name}"
                    )
                    vm.onTabSelected(it)
                }
            }
        })

        sliding_tabs.setupWithViewPager(viewpager)
        for (i in 0 until sliding_tabs.tabCount) {
            sliding_tabs.getTabAt(i)?.icon =
                ResourcesCompat.getDrawable(
                    resources,
                    tabs[i].iconRes,
                    requireContext().theme
                )
        }
    }

}
