package com.hypertrack.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.hypertrack.android.ui.base.NavActivity
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.NavGraphDirections
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : NavActivity(), DeeplinkResultListener {

    val splashScreenViewModel: SplashScreenViewModel by viewModels {
        MyApplication.injector.provideViewModelFactory(MyApplication.context)
    }

    private val deepLinkProcessor = MyApplication.injector.deeplinkProcessor
    private val crashReportsProvider = MyApplication.injector.crashReportsProvider

    override val layoutRes: Int = R.layout.activity_main

    override val navHostId: Int = R.id.navHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tvMockMode.setGoneState(MyApplication.MOCK_MODE.not())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_SYNC) {
            val currentFragment = getCurrentFragment()
            if (currentFragment is VisitsManagementFragment) {
                //todo test
                currentFragment.refreshOrders()
            } else {
                findNavController(R.id.root).navigate(NavGraphDirections.actionGlobalVisitManagementFragment())
            }
        } else {
            deepLinkProcessor.activityOnNewIntent(this, this)
        }
    }

    override fun onStart() {
        super.onStart()
        deepLinkProcessor.activityOnStart(this, this)
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        crashReportsProvider.log("activity resume")
    }

    override fun onPause() {
        inForeground = false
        crashReportsProvider.log("activity pause")
        super.onPause()
    }

    override fun onDeeplinkResult(result: DeeplinkResult) {
        splashScreenViewModel.handleDeeplink(result, this)
    }

    override fun onDestinationChanged(destination: NavDestination) {
        crashReportsProvider.log("Destination changed: ${destination.label.toString()}")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        getCurrentFragment().onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        getCurrentFragment().onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (getCurrentBaseFragment()?.onBackPressed() == false) {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        var inForeground: Boolean = false
    }
}