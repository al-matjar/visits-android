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
    private val vm: ActivityViewModel by viewModels {
        MyApplication.newInjector.provideActivityViewModelFactory()
    }

    override val layoutRes: Int = R.layout.activity_main

    override val navHostId: Int = R.id.navHost

    override fun onCreate(savedInstanceState: Bundle?) {
        // restoring activity state is disabled because it can restore
        // fragments in wrong app state
        super.onCreate(null)

        withErrorHandling(vm::onError) {
            vm.navigationEvent.observeWithErrorHandling(
                this,
                vm::onError,
            ) {
                navController.navigate(it)
            }

            vm.showErrorMessageEvent.observeWithErrorHandling(
                this,
                vm::onError,
            ) {
                SnackBarUtil.showErrorSnackBar(root, it)
            }

            vm.showAppMessageEvent.observeWithErrorHandling(
                this,
                vm::onError,
            ) {
                SnackBarUtil.showSnackBar(root, it)
            }

            // todo test current fragment null
            vm.onCreate(intent, getCurrentFragment())
        }
    }

    override fun onStart() {
        super.onStart()
        withErrorHandling(vm::onError) {
            crashReportsProvider.log("activity start")
            vm.onStart(this, intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        withErrorHandling(vm::onError) {
            // from branch tutorial, do we need this?
            setIntent(intent)
            vm.onNewIntent(intent, this, getCurrentFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        withErrorHandling(vm::onError) {
            inForeground = true
            crashReportsProvider.log("activity resume")
        }
    }

    override fun onPause() {
        withErrorHandling(vm::onError) {
            inForeground = false
            crashReportsProvider.log("activity pause")
            super.onPause()
        }
    }

    override fun onStop() {
        super.onStop()
        withErrorHandling(vm::onError) {
            crashReportsProvider.log("activity stop")
        }
    }

    override fun onDestinationChanged(destination: NavDestination) {
        vm.onNavDestinationChanged(destination)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        withErrorHandling(vm::onError) {
            getCurrentFragment().onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        withErrorHandling(vm::onError) {
            getCurrentFragment().onActivityResult(requestCode, resultCode, data)
            // todo to use case
            if (requestCode == REQUEST_CODE_UPDATE) {
                when (resultCode) {
                    RESULT_CANCELED, RESULT_OK -> {
                    }
                    else -> {
                        crashReportsProvider.logException(Exception("Update failed: resultCode=$resultCode, data=$data"))
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        withErrorHandling(vm::onError) {
            if (getCurrentBaseFragment()?.onBackPressed() == false) {
                super.onBackPressed()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        withErrorHandling(vm::onError) {
            onBackPressed()
        }
        return true
    }

    override fun toString(): String = javaClass.simpleName

    companion object {
        var inForeground: Boolean = false

        const val REQUEST_CODE_IMAGE_CAPTURE = 1
        const val REQUEST_CODE_UPDATE = 2
    }
}

