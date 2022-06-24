package com.hypertrack.android.ui.activity

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.AppState
import com.hypertrack.android.interactors.app.ActivityOnNewIntent
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.ui.activity.use_case.HandleDeeplinkResultUseCase
import com.hypertrack.android.ui.activity.use_case.HandleNotificationClickUseCase
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.withErrorHandling
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.catchException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception

class ActivityViewModel(
    private val appInteractor: AppInteractor,
    private val appState: LiveData<AppState>,
    private val crashReportsProvider: CrashReportsProvider,
    private val branchWrapper: BranchWrapper,
    private val appCoroutineScope: CoroutineScope,
    private val getErrorMessageUseCase: GetErrorMessageUseCase,
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase
) : ViewModel() {

    val navigationEvent = MediatorLiveData<Consumable<NavDirections>>().apply {
        addSource(appInteractor.navigationEvent) {
            postValue(it)
        }
    }
    val showErrorMessageEvent = Transformations.switchMap(appInteractor.appErrorEvent) {
        MutableLiveData<Consumable<ErrorMessage>>().apply {
            it.consume {
                appCoroutineScope.launch {
                    getErrorMessageUseCase.execute(ExceptionError(it)).collect {
                        postValue(it)
                    }
                }
            }
        }
    }

    // todo navigate via global nav event
    private val handleNotificationClickUseCase = HandleNotificationClickUseCase(
        navigationEvent
    )
    private val handleDeeplinkResultUseCase = HandleDeeplinkResultUseCase(
        getBranchDataFromAppBackendUseCase
    )
    private val deeplinkDelegate = DeeplinkDelegate(appCoroutineScope, branchWrapper)

    init {
        runInVmEffectsScope {
            deeplinkDelegate.deeplinkFlow.catchException {
                onError(it)
            }.collect {
                handleEffect(handleDeeplinkResultUseCase.execute(it))
            }
        }
    }

    fun onCreate(intent: Intent?, currentFragment: Fragment) {
        intent?.let {
            ifLoggedIn {
                handleEffect(handleNotificationClickUseCase.execute(it, intent, currentFragment))
            }
        }
    }

    fun onStart(activity: Activity, intent: Intent?) {
        // no need for DeeplinkCheckStartedAction because it
        // either called with SplashScreen or onNewIntent will be called anyway
        deeplinkDelegate.onActivityStart(activity, intent)
        withErrorHandling(this::onError) {
            showPermissionPrompt()
        }
    }

    fun onNewIntent(
        intent: Intent?,
        activity: Activity,
        currentFragment: Fragment
    ) {
        intent?.let {
            if (isFromPushMessage(intent)) {
                ifLoggedIn {
                    handleEffect(
                        handleNotificationClickUseCase.execute(
                            it,
                            intent,
                            currentFragment
                        )
                    )
                }
            } else {
                deeplinkDelegate.onActivityNewIntent(activity, intent).also {
                    // to show progressbar
                    appInteractor.handleAction(ActivityOnNewIntent(intent))
                }
            }
        }
    }

    fun onNavDestinationChanged(destination: NavDestination) {
        crashReportsProvider.log("Destination changed: ${destination.label.toString()}")
    }

    fun onError(exception: Exception) {
        appInteractor.handleAction(AppErrorAction(exception))
    }

    private fun handleEffect(effectFlow: Flow<AppAction?>) {
        runInVmEffectsScope {
            effectFlow
                .catchException { e ->
                    onError(e)
                }
                .collect {
                    it?.let { appInteractor.handleAction(it) }
                }
        }
    }

    private fun isFromPushMessage(intent: Intent): Boolean {
        return intent.hasExtra(NotificationUtil.KEY_NOTIFICATION_TYPE)
    }

    private fun showPermissionPrompt() {
        when (val state = appState.requireValue()) {
            is Initialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState.userScope.hyperTrackService.showPermissionsPrompt()
                    }
                    UserNotLoggedIn -> {
                    }
                }
            }
            is NotInitialized -> {
            }
        }
    }

    private fun ifLoggedIn(block: (UserLoggedIn) -> Unit) {
        appState.requireValue().let {
            when (it) {
                is Initialized -> {
                    when (it.userState) {
                        is UserLoggedIn -> {
                            block.invoke(it.userState)
                        }
                        UserNotLoggedIn -> {
                        }
                    }
                }
                is NotInitialized -> {
                }
            }
        }
    }

    private fun runInVmEffectsScope(block: suspend CoroutineScope.() -> Unit) {
        appCoroutineScope.launch(block = block)
    }
}

@Suppress("UNCHECKED_CAST")
class ActivityViewModelFactory(
    private val appInteractor: AppInteractor,
    private val appScope: AppScope,
    private val useCases: UseCases,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActivityViewModel(
            appInteractor,
            appInteractor.appState,
            appScope.crashReportsProvider,
            appScope.branchWrapper,
            appScope.appCoroutineScope,
            useCases.getErrorMessageUseCase,
            useCases.getBranchDataFromAppBackendUseCase
        ) as T
    }
}
