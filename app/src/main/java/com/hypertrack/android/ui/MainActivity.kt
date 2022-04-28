package com.hypertrack.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.ui.base.NavActivity
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.use_case.HandlePushUseCase
import com.hypertrack.android.use_case.OutageNotification
import com.hypertrack.android.use_case.TripUpdateNotification
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.NotificationUtil.KEY_NOTIFICATION_DATA
import com.hypertrack.android.utils.NotificationUtil.KEY_NOTIFICATION_TYPE
import com.hypertrack.logistics.android.github.NavGraphDirections
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : NavActivity() {

    val splashScreenViewModel: SplashScreenViewModel by viewModels {
        MyApplication.injector.provideViewModelFactory(MyApplication.context)
    }

    private val deepLinkProcessor = MyApplication.injector.deeplinkProcessor
    private val crashReportsProvider = MyApplication.injector.crashReportsProvider

    override val layoutRes: Int = R.layout.activity_main

    override val navHostId: Int = R.id.navHost

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try {
            intent?.let {
                handleNotification()
                lifecycleScope.launch {
                    deepLinkProcessor.activityOnNewIntent(this@MainActivity).let {
                        onDeeplinkResult(it)
                    }
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleNotification()
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            lifecycleScope.launch {
                deepLinkProcessor.activityOnStart(this@MainActivity).let {
                    onDeeplinkResult(it)
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
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

    fun onDeeplinkResult(result: DeeplinkResult) {
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

    private fun handleNotification() {
        intent.getStringExtra(KEY_NOTIFICATION_TYPE)?.let { type ->
            when (type) {
                TripUpdateNotification::class.java.simpleName -> {
                    val currentFragment = getCurrentFragment()
                    if (currentFragment is VisitsManagementFragment) {
                        currentFragment.refreshOrders()
                    } else {
                        navController.navigate(
                            NavGraphDirections.actionGlobalVisitManagementFragment()
                        )
                    }
                }
                OutageNotification::class.java.simpleName -> {
                    val notificationData = intent.getParcelableExtra<OutageNotification>(
                        KEY_NOTIFICATION_DATA
                    )
                    if (notificationData != null) {
                        navController.navigate(
                            NavGraphDirections.actionGlobalOutageFragment(notificationData)
                        )
                    } else {
                        crashReportsProvider.logException(NullPointerException())
                    }
                }
            }
            intent.removeExtra(KEY_NOTIFICATION_TYPE)
            intent.removeExtra(KEY_NOTIFICATION_DATA)
        }
    }

    companion object {
        var inForeground: Boolean = false
    }
}
