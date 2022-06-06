package com.hypertrack.android.interactors.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.di.Injector.crashReportsProvider
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.use_case.app.InitAppUseCase
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class AppInteractor(
    private val appScope: AppScope,
) {

    private val effectsDispatcher = Dispatchers.Default
    private val useCases = UseCases(appScope, this, MyApplication.SERVICES_API_KEY)
    private val initialState = NotInitialized(
        appScope,
        useCases,
        viewState = SplashScreenState,
        pendingDeeplinkResult = null,
        pendingPushNotification = null
    )
    private val appReducer = AppReducer(useCases, appScope)
    private val appStateMachine = StateMachine<AppAction, AppState, Effect>(
        "AppState",
        appScope.crashReportsProvider,
        initialState,
        appScope.appCoroutineScope,
        Dispatchers.Default,
        appReducer::reduce,
        this::applyEffects,
        this::stateChangeEffects
    )

    private val _appState = MutableLiveData<AppState>(initialState)
    val appState: LiveData<AppState> = _appState

    private val _appErrorEvent = MutableLiveData<Consumable<Exception>>()
    val appErrorEvent: LiveData<Consumable<Exception>> = _appErrorEvent

    private val _navigationEvent = MutableLiveData<Consumable<NavDirections>>()
    val navigationEvent: LiveData<Consumable<NavDirections>> = _navigationEvent

    fun handleAction(action: AppAction) {
        appStateMachine.handleAction(action)
    }

    private fun applyEffects(effects: Set<Effect>) {
        effects.forEach { effect ->
            appScope.appCoroutineScope.launch {
                getEffectFlow(effect)
                    .catch { e ->
                        appScope.crashReportsProvider.logException(e)
                        if (e is Exception) {
                            emit(AppErrorAction(e))
                        } else throw e
                    }
                    .collect { action ->
                        action?.let {
                            handleAction(it)
                        }
                    }
            }
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<AppAction?> {
        return when (effect) {
            is InitAppEffect -> {
                InitAppUseCase(useCases.getConfiguredHypertrackSdkInstanceUseCase)
                    .execute(effect.appScope).flowOn(effectsDispatcher)
                    .flatMapConcat { userLoginStatus ->
                        useCases.loadUserStateUseCase
                            .execute(userLoginStatus).flowOn(Dispatchers.Main)
                            .flatMapConcat { result ->
                                when (result) {
                                    is Success -> {
                                        flowOf(AppInitializedAction(result.data))
                                    }
                                    is Failure -> {
                                        getEffectFlow(
                                            ShowAppErrorMessageEffect(result.exception)
                                        ).map { AppInitializedAction(UserNotLoggedIn) }
                                    }
                                }
                            }
                    }
            }
            is LoginWithDeeplinkEffect -> {
                useCases.loginWithDeeplinkUseCase.execute(effect.deeplinkParams)
                    .flowOn(effectsDispatcher)
                    .flatMapConcat {
                        when (it) {
                            is AbstractSuccess -> {
                                useCases.loadUserStateAfterSignInUseCase
                                    .execute(it.success).flowOn(Dispatchers.Main)
                                    .map { result ->
                                        when (result) {
                                            is Success -> {
                                                SignedInAction(result.data)
                                            }
                                            is Failure -> {
                                                DeeplinkLoginErrorAction(result.exception)
                                            }
                                        }
                                    }
                            }
                            is AbstractFailure -> {
                                it.failure.failure.toException().let {
                                    DeeplinkLoginErrorAction(it)
                                }.toFlow()
                            }
                        }
                    }
            }
            is CleanupUserScopeEffect -> {
                { effect.oldUserScope.onDestroy() }
                    .asFlow()
                    .map { null }
            }
            is HandlePushEffect -> {
                effect.userState.userScope
                    .handlePushUseCase.execute(effect.userState, effect.remoteMessage).map { null }
            }
            is SetCrashReportingDeviceIdentifier -> {
                useCases.setCrashReportingIdUseCase.execute(
                    effect.deviceId,
                ).map { null }
            }
            is NotifyAppStateUpdateEffect -> {
                { _appState.postValue(effect.newState) }.asFlow().map { null }
            }
            is ShowAppErrorMessageEffect -> {
                {
                    if (MyApplication.DEBUG_MODE) {
                        effect.exception.printStackTrace()
                    }
                    appScope.crashReportsProvider.logException(effect.exception)
                    _appErrorEvent.postValue(effect.exception.toConsumable())
                }.asFlow().map { null }
            }
            is NavigateToUserScopeScreensEffect -> {
                useCases.navigateToUserScopeScreensUseCase.execute(
                    effect.newUserState.userScope.permissionsInteractor
                ).map {
                    when (it) {
                        is Success -> {
                            _navigationEvent.postValue(it.data.toConsumable())
                            null
                        }
                        is Failure -> {
                            AppErrorAction(it.exception)
                        }
                    }
                }
            }
            NavigateToSignInEffect -> {
                {
                    _navigationEvent.postValue(
                        NavGraphDirections.actionGlobalSignInFragment().toConsumable()
                    )
                }.asFlow().map { null }
            }
        }
    }

    // todo
    // notifying app state changed can be delayed
    // consider to change this logic if any client needs to receive app state
    // in "get" mode (but not "event" mode)
    private fun stateChangeEffects(newState: AppState): Set<Effect> {
        return setOf(NotifyAppStateUpdateEffect(newState))
    }

    companion object {
        const val CHANNEL_ID = "default_notification_channel"
        const val IMPORTANT_CHANNEL_ID = "important_notification_channel"
    }

    override fun toString(): String = javaClass.simpleName
}

fun tryWithAppError(appInteractor: AppInteractor, block: () -> Unit) {
    try {
        block.invoke()
    } catch (e: Exception) {
        appInteractor.handleAction(AppErrorAction(e))
    }
}
