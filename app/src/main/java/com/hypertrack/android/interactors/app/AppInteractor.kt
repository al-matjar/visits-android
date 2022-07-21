package com.hypertrack.android.interactors.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.action.GeofencesForMapLoadedAction
import com.hypertrack.android.interactors.app.effect.HistoryEffectHandler
import com.hypertrack.android.interactors.app.effect.MapEffectsHandler
import com.hypertrack.android.interactors.app.reducer.GeofencesForMapReducer
import com.hypertrack.android.interactors.app.reducer.HistoryReducer
import com.hypertrack.android.interactors.app.reducer.HistoryViewReducer
import com.hypertrack.android.interactors.app.reducer.ScreensReducer
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.use_case.app.InitAppUseCase
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class AppInteractor(
    val appScope: AppScope,
) {

    private val effectsDispatcher = Dispatchers.Default
    private val useCases = UseCases(appScope, this, MyApplication.SERVICES_API_KEY)
    private val initialState = AppNotInitialized(
        appScope,
        useCases,
        splashScreenViewState = null,
        pendingDeeplinkResult = null,
        pendingPushNotification = null
    )
    private val appReducer = AppReducer(
        useCases,
        appScope,
        HistoryReducer(appScope, HistoryViewReducer(appScope)),
        HistoryViewReducer(appScope),
        ScreensReducer(HistoryViewReducer(appScope)),
        GeofencesForMapReducer()
    )
    private val appStateMachine = object : StateMachine<AppAction, AppState, AppEffect>(
        "AppState",
        appScope.crashReportsProvider,
        initialState,
        appScope.appCoroutineScope,
        appScope.stateMachineContext,
        appReducer::reduce,
        this::applyEffects,
        this::stateChangeEffects
    ) {}
    private val historyEffectHandler = HistoryEffectHandler(appScope, useCases)
    private val mapEffectsHandler = MapEffectsHandler(
        useCases.logExceptionIfFailureUseCase,
        this::getEffectFlow
    )

    private val _appState = MutableLiveData<AppState>(initialState)
    val appState: LiveData<AppState> = _appState

    // todo migrate to flow from livedata
    private val _appStateFlow = MutableStateFlow<AppState>(initialState)
    val appStateFlow: StateFlow<AppState> = _appStateFlow

    private val _appEvent = MutableSharedFlow<AppEvent>(replay = 0)
    val appEvent: Flow<AppEvent> = _appEvent

    // todo register activity event handle via action
    private val _appErrorEvent = MutableLiveData<Consumable<Exception>>()
    val appErrorEvent: LiveData<Consumable<Exception>> = _appErrorEvent

    // todo register activity event handle via action
    private val _navigationEvent = MutableLiveData<Consumable<NavDirections>>()
    val navigationEvent: LiveData<Consumable<NavDirections>> = _navigationEvent

    fun handleAction(action: AppAction) {
        appStateMachine.handleAction(action)
    }

    fun handleActionFlow(action: AppAction): Flow<Unit> {
        return {
            appStateMachine.handleAction(action)
        }.asFlow()
    }

    private fun applyEffects(effects: Set<AppEffect>) {
        effects.forEach { effect ->
            appScope.appCoroutineScope.launch {
                getEffectFlow(effect)
                    .catch { e ->
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

    private fun getEffectFlow(effect: AppEffect): Flow<AppAction?> {
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
                                        AppInitializedAction(result.data).toFlow()
                                    }
                                    is Failure -> {
                                        getEffectFlow(
                                            ShowAndReportAppErrorEffect(result.exception)
                                        ).map { AppInitializedAction(UserNotLoggedIn) }
                                    }
                                }
                            }
                    }
            }
            is LoginWithDeeplinkEffect -> {
                useCases.loginWithDeeplinkParamsUseCase.execute(effect.deeplinkParams)
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
                { effect.oldUserScope.onDestroy() }.asFlow().noAction()
            }
            is HandlePushEffect -> {
                effect.userState.userScope
                    .handlePushUseCase.execute(effect.userState, effect.remoteMessage).noAction()
            }
            is NotifyAppStateUpdateEffect -> {
                suspend {
                    _appState.postValue(effect.newState)
                    _appStateFlow.emit(effect.newState)
                }.asFlow().noAction()
            }
            is ShowAndReportAppErrorEffect -> {
                {
                    appScope.crashReportsProvider.logException(effect.exception)
                    _appErrorEvent.postValue(effect.exception.toConsumable())
                }.asFlow().noAction()
            }
            is ReportAppErrorEffect -> {
                {
                    appScope.crashReportsProvider.logException(effect.exception)
                }.asFlow().noAction()
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
            is LoadHistoryEffect -> {
                historyEffectHandler.applyEffect(effect)
            }
            is AppEventEffect -> {
                getEffectFlow(effect)
            }
            is HistoryViewEffect -> {
                historyEffectHandler.applyEffect(effect)
            }
            is AppMapEffect -> {
                mapEffectsHandler.applyEffect(effect)
            }
            is AppActionEffect -> {
                { effect.action }.asFlow()
            }
            is NavigateEffect -> {
                _navigationEvent.updateConsumableAsFlow(effect.destination).noAction()
            }
            is LoadGeofencesForMapEffect -> {
                effect.useCases.loadGeofencesForMapUseCase.execute(effect.geoHash, effect.pageToken)
                    .map {
                        GeofencesForMapAppAction(
                            GeofencesForMapLoadedAction(
                                effect.geoHash,
                                it
                            )
                        )
                    }
            }
        }
    }

    // todo notifying app state change can be delayed
    // consider to change this logic if any client needs to receive app state
    // in "get" mode (but not "event" mode)
    private fun stateChangeEffects(newState: AppState): Set<AppEffect> {
        return setOf(NotifyAppStateUpdateEffect(newState))
    }


    private fun getEffectFlow(effect: AppEventEffect): Flow<AppAction?> {
        return suspend { _appEvent.emit(effect.event) }.asFlow().noAction()
    }

    companion object {
        const val CHANNEL_ID = "default_notification_channel"
        const val IMPORTANT_CHANNEL_ID = "important_notification_channel"
    }

    override fun toString(): String = javaClass.simpleName
}

// don't use .map { null } on arbitrary typed Flow to avoid bugs
// e.g. missing error handling
// set flow to Unit, and then map to no action
fun <T> Flow<Unit>.noAction(): Flow<T?> {
    return map { null }
}


