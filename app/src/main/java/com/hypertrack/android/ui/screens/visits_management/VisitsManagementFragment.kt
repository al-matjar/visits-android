package com.hypertrack.android.ui.screens.visits_management

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.hypertrack.android.models.Visit
import com.hypertrack.android.models.VisitStatusGroup
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalGeofenceJson
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.NotificationUtils
import com.hypertrack.android.ui.common.SimplePageChangedListener
import com.hypertrack.android.ui.common.SnackbarUtil
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapViewFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapViewFragmentOld
import com.hypertrack.android.ui.screens.visits_management.tabs.livemap.LiveMapFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.visits.VisitListAdapter
import com.hypertrack.android.ui.screens.visits_management.tabs.visits.VisitsListFragment
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_signup.*
import kotlinx.android.synthetic.main.fragment_visits_management.*

class VisitsManagementFragment : ProgressDialogFragment(R.layout.fragment_visits_management) {

    val args: VisitsManagementFragmentArgs by navArgs()

    private val tabsMap = mapOf(
        Tab.MAP to if (MyApplication.TWMO_ENABLED) {
            CurrentTripFragment()
        } else {
            Injector.getCustomFragmentFactory(MyApplication.context)
                .instantiate(ClassLoader.getSystemClassLoader(), LiveMapFragment::class.java.name)
        },
        Tab.HISTORY to MapViewFragment(),
        Tab.ORDERS to OrdersFragment.newInstance(),
        Tab.VISITS to VisitsListFragment.newInstance(),
        Tab.PLACES to PlacesFragment.getInstance(),
        Tab.SUMMARY to SummaryFragment.newInstance(),
        Tab.PROFILE to ProfileFragment()
    )
    private val tabs = Injector.provideTabs()

    val visitsManagementViewModel: VisitsManagementViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkInvariants()

        visitsManagementViewModel.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        viewAdapter = VisitListAdapter(
            visitsManagementViewModel.visits,
            object : VisitListAdapter.OnListAdapterClick {
                override fun onJobItemClick(position: Int) {
                    val visit = visitsManagementViewModel.visits.value?.get(position)
                    visit?.let {
                        if (it is Visit) {
                            findNavController().navigate(
                                VisitsManagementFragmentDirections.actionVisitManagementFragmentToVisitDetailsFragment(
                                    it._id
                                )
                            )
                        }
                    }
                }
            }
        )

        visitsManagementViewModel.visits.observe(viewLifecycleOwner, {
            viewAdapter.notifyDataSetChanged()
            viewAdapter.placeholderListener?.invoke(it.isEmpty())
        })

        viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getCount(): Int = tabs.size

            override fun getItem(position: Int): Fragment {
                val fragment = tabsMap.getValue(tabs[position])
                if (fragment is MapViewFragmentOld) {
                    fragment.arguments = Bundle().apply {
                        putString(
                            MapViewFragmentOld.WEBVIEW_URL,
                            visitsManagementViewModel.deviceHistoryWebUrl
                        )
                    }
                }
                return fragment
            }
        }
        viewpager.addOnPageChangeListener(object : SimplePageChangedListener() {
            override fun onPageSelected(position: Int) {
                MyApplication.injector.crashReportsProvider.log(
                    "Tab selected ${tabs[position].name}"
                )
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

        visitsManagementViewModel.statusBarColor.observe(viewLifecycleOwner) { color ->
            tvTrackerStatus.visibility = if (color == null) View.GONE else View.VISIBLE
            color?.let { tvTrackerStatus.setBackgroundColor(requireContext().getColor(it)) }
        }

        visitsManagementViewModel.statusBarMessage.observe(viewLifecycleOwner) { msg ->
            when (msg) {
                is StatusString -> tvTrackerStatus.setText(msg.stringId)
            }
        }

        visitsManagementViewModel.showSpinner.observe(viewLifecycleOwner) { show ->
            if (show) showProgress() else dismissProgress()
        }
        visitsManagementViewModel.showSync.observe(viewLifecycleOwner) { show ->
            if (show) {
                NotificationUtils.showSyncNotification(requireContext())
            } else {
                NotificationUtils.dismissSyncNotification()
            }
        }
        visitsManagementViewModel.enableCheckIn.observe(viewLifecycleOwner) { enabled ->
            checkIn.isEnabled = enabled
        }
        if (visitsManagementViewModel.showCheckIn)
            checkIn.visibility = View.VISIBLE
        else
            checkIn.visibility = View.GONE

        visitsManagementViewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            swClockIn.setStateWithoutTriggeringListener(isTracking)
            tvClockHint.setText(
                if (isTracking) {
                    R.string.clock_hint_tracking_on
                } else {
                    R.string.clock_hint_tracking_off
                }
            )
        }
        visitsManagementViewModel.checkInButtonText.observe(viewLifecycleOwner) { label ->
            checkIn.text = when (label) {
                LocalVisitCtaLabel.CHECK_OUT -> getString(R.string.check_out)
                else -> getString(R.string.check_in)
            }
        }

        swClockIn.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked != visitsManagementViewModel.isTracking.value) {
                visitsManagementViewModel.switchTracking()
            }
        }
        checkIn.setOnClickListener { visitsManagementViewModel.checkIn() }
        visitsManagementViewModel.showToast.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Toast
                .makeText(requireContext(), msg, Toast.LENGTH_LONG)
                .show()
        }

        visitsManagementViewModel.error.observe(viewLifecycleOwner, { error ->
            SnackbarUtil.showErrorSnackbar(view, error)
        })

        //moved from onActivityResult
        visitsManagementViewModel.possibleLocalVisitCompletion()

        visitsManagementViewModel.refreshHistory()

        args.tab?.let { tab ->
            viewpager.currentItem = tabs.indexOf(args.tab)
        }
    }

    lateinit var viewAdapter: VisitListAdapter

    override fun onResume() {
        super.onResume()
        refreshVisits()
    }

    fun refreshVisits() {
        visitsManagementViewModel.refreshVisits { }
    }

    private fun checkInvariants() {
        if (BuildConfig.DEBUG) {
            if (resources.getStringArray(R.array.visit_state_group_names).size != VisitStatusGroup.values().size) {
                error(
                    "visit_state_group_names array doesn't contain enough members to represent " +
                            "all the VisitStatusGroup values"
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        visitsManagementViewModel.showSync.value?.let {
            if (it) {
                NotificationUtils.dismissSyncNotification()
            }
        }
    }

    companion object {
        const val TAG = "VisitsManagementAct"
    }

}

