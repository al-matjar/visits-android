package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.background_permissions.BackgroundPermissionsViewModel
import com.hypertrack.android.ui.screens.confirm_email.ConfirmEmailViewModel
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.ui.screens.sign_up.SignUpViewModel
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenViewModel
import com.hypertrack.android.utils.AppScope
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.Moshi

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appScope: AppScope,
    private val permissionsInteractor: PermissionsInteractor,
    private val crashReportsProvider: CrashReportsProvider,
    private val osUtilsProvider: OsUtilsProvider,
    private val moshi: Moshi,
    private val hyperTrackServiceProvider: () -> HyperTrackService?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseDependencies = BaseViewModelDependencies(
            osUtilsProvider,
            osUtilsProvider,
            crashReportsProvider
        )
        return when (modelClass) {
            ConfirmEmailViewModel::class.java -> ConfirmEmailViewModel(
                baseDependencies,
                appScope.loginInteractor,
                permissionsInteractor,
            ) as T
            SignInViewModel::class.java -> SignInViewModel(
                baseDependencies,
                appScope.loginInteractor,
                permissionsInteractor,
                appScope.deeplinkInteractor,
                appScope.deeplinkProcessor,
                appScope.moshi
            ) as T
            SignUpViewModel::class.java -> SignUpViewModel(
                baseDependencies,
                appScope.loginInteractor,
            ) as T
            SplashScreenViewModel::class.java -> SplashScreenViewModel(
                baseDependencies,
                appScope.deeplinkInteractor,
                permissionsInteractor,
                hyperTrackServiceProvider
            ) as T
            BackgroundPermissionsViewModel::class.java -> BackgroundPermissionsViewModel(
                baseDependencies,
                permissionsInteractor
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
