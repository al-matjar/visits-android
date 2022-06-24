package com.hypertrack.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavDestination
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.activity.ActivityViewModel
import com.hypertrack.android.ui.base.NavActivity
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.base.withErrorHandling
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : NavActivity() {

    private val crashReportsProvider = Injector.crashReportsProvider
    private val activityViewModel: ActivityViewModel by viewModels {
        MyApplication.newInjector.provideActivityViewModelFactory()
    }

    override val layoutRes: Int = R.layout.activity_main

    override val navHostId: Int = R.id.navHost

    override fun onCreate(savedInstanceState: Bundle?) {
        // restoring activity state is disabled because it can restore
        // fragments in wrong app state
        super.onCreate(null)

        withErrorHandling(activityViewModel::onError) {
            activityViewModel.navigationEvent.observeWithErrorHandling(
                this,
                activityViewModel::onError,
            ) {
                navController.navigate(it)
            }

            activityViewModel.showErrorMessageEvent.observeWithErrorHandling(
                this,
                activityViewModel::onError,
            ) {
                SnackBarUtil.showErrorSnackBar(root, it)
            }

            // todo test current fragment null
            activityViewModel.onCreate(intent, getCurrentFragment())
        }
    }

    override fun onStart() {
        super.onStart()
        withErrorHandling(activityViewModel::onError) {
            crashReportsProvider.log("activity start")
            activityViewModel.onStart(this, intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        withErrorHandling(activityViewModel::onError) {
            // from branch tutorial, do we need this?
            setIntent(intent)
            activityViewModel.onNewIntent(intent, this, getCurrentFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        withErrorHandling(activityViewModel::onError) {
            inForeground = true
            crashReportsProvider.log("activity resume")
        }
    }

    override fun onPause() {
        withErrorHandling(activityViewModel::onError) {
            inForeground = false
            crashReportsProvider.log("activity pause")
            super.onPause()
        }
    }

    override fun onStop() {
        super.onStop()
        withErrorHandling(activityViewModel::onError) {
            crashReportsProvider.log("activity stop")
        }
    }

    override fun onDestinationChanged(destination: NavDestination) {
        activityViewModel.onNavDestinationChanged(destination)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        withErrorHandling(activityViewModel::onError) {
            getCurrentFragment().onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        withErrorHandling(activityViewModel::onError) {
            getCurrentFragment().onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        withErrorHandling(activityViewModel::onError) {
            if (getCurrentBaseFragment()?.onBackPressed() == false) {
                super.onBackPressed()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        withErrorHandling(activityViewModel::onError) {
            onBackPressed()
        }
        return true
    }

    override fun toString(): String = javaClass.simpleName

    companion object {
        var inForeground: Boolean = false
    }
}
