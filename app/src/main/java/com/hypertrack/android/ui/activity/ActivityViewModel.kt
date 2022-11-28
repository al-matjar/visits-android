package com.hypertrack.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import com.google.android.a.a
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.ActivityOnNewIntent
import com.hypertrack.android.interactors.app.AppEvent
import com.hypertrack.android.interactors.app.AppMessageEvent
import com.hypertrack.android.ui.activity.use_case.HandleDeeplinkResultUseCase
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.activity.use_case.HandleNotificationClickUseCase
import com.hypertrack.android.ui.activity.use_case.CheckForUpdatesUseCase
import com.hypertrack.android.ui.activity.use_case.RequestUpdateIfAvailableUseCase
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.withErrorHandling
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.use_case.app.threading.EffectsScope
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication.Companion.context
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.catchException
import io.sentry.core.protocol.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.Exception
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.mapAppErrorAction

class ActivityViewModel(
    private val appInteractor: AppInteractor,
    private val appState: LiveData<AppState>,
    private val crashReportsProvider: CrashReportsProvider,
    private val branchWrapper: BranchWrapper,
    private val effectsScope: EffectsScope,
    private val getErrorMessageUseCase: GetErrorMessageUseCase,
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase,
    private val validateDeeplinkUrlUseCase: ValidateDeeplinkUrlUseCase
) : ViewModel() {

    val navigationEvent = MediatorLiveData<Consumable<NavDirections>>().apply {
        addSource(appInteractor.navigationEvent) {
            postValue(it)
        }
    }
    val showAppMessageEvent = MediatorLiveData<String>().apply {
        runInVmEffectsScope {
            appInteractor.appEvent.collect {
                if (it is AppMessageEvent) {
                    postValue(it.message)
                }
            }
        }
    }
    val showErrorMessageEvent = Transformations.switchMap(appInteractor.appErrorEvent) {
        MutableLiveData<Consumable<ErrorMessage>>().apply {
            it.consume {
                effectsScope.value.launch {
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
        getBranchDataFromAppBackendUseCase,
        validateDeeplinkUrlUseCase
    )
    private val requestUpdateIfAvailableUseCase = RequestUpdateIfAvailableUseCase(
        CheckForUpdatesUseCase()
    )

    private val deeplinkDelegate =
        DeeplinkDelegate(effectsScope.value, appInteractor, branchWrapper)

    init {
        runInVmEffectsScope {
            deeplinkDelegate.deeplinkFlow.catchException {
                handleEffect(
                    handleDeeplinkResultUseCase.execute(DeeplinkError(it, null))
                        .mapAppErrorAction()
                )
            }.collect {
                handleEffect(
                    handleDeeplinkResultUseCase.execute(it)
                        .mapAppErrorAction()
                )
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

        runInVmEffectsScope {
            handleEffect(requestUpdateIfAvailableUseCase.execute(activity))
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
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState.userScope.hyperTrackService.showPermissionsPrompt()
                    }
                    UserNotLoggedIn -> {
                    }
                }
            }
            is AppNotInitialized -> {
            }
        }
    }

    private fun ifLoggedIn(block: (UserLoggedIn) -> Unit) {
        appState.requireValue().let {
            when (it) {
                is AppInitialized -> {
                    when (it.userState) {
                        is UserLoggedIn -> {
                            block.invoke(it.userState)
                        }
                        UserNotLoggedIn -> {
                        }
                    }
                }
                is AppNotInitialized -> {
                }
            }
        }
    }

    private fun runInVmEffectsScope(block: suspend CoroutineScope.() -> Unit) {
        effectsScope.value.launch(block = block)
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
            appScope.effectsScope,
            useCases.getErrorMessageUseCase,
            useCases.getBranchDataFromAppBackendUseCase,
            useCases.validateDeeplinkUrlUseCase
        ) as T
    }
}
