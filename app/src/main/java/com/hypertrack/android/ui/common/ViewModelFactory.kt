package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.screens.confirm_email.ConfirmEmailViewModel
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.use_case.app.UseCases

// todo set separate factories for all vms
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val useCases: UseCases
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val baseViewModelDependencies = BaseViewModelDependencies(
            appScope.osUtilsProvider,
            appScope.osUtilsProvider,
            appScope.crashReportsProvider
        )
        return when (modelClass) {
            ConfirmEmailViewModel::class.java -> ConfirmEmailViewModel(
                baseViewModelDependencies,
                appInteractor,
                useCases.verifyByOtpCodeUseCase,
                useCases.resendExceptionToCrashlyticsUseCase,
                useCases.loadUserStateAfterSignInUseCase,
            ) as T
            SignInViewModel::class.java -> SignInViewModel(
                baseViewModelDependencies,
                appInteractor,
                useCases.signInUseCase,
                useCases.loginWithDeeplinkUseCase,
                useCases.loadUserStateAfterSignInUseCase,
                useCases.logExceptionToCrashlyticsUseCase,
                useCases.logMessageToCrashlyticsUseCase,
                appScope.branchWrapper,
                appScope.moshi
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}
