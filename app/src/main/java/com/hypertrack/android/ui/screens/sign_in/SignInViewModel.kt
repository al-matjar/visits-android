package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.sign_in.use_case.ConfirmationRequired
import com.hypertrack.android.ui.screens.sign_in.use_case.ConfirmationRequiredUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandleDeeplinkFailureUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandlePastedDeeplinkOrTokenUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandleSignInUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInSuccess
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInWithCognitoUseCase
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.app.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.DeeplinkException
import com.hypertrack.android.use_case.deeplink.DeeplinkValidationError
import com.hypertrack.android.use_case.login.LoadUserStateAfterSignInUseCase
import com.hypertrack.android.use_case.login.LoggedIn
import com.hypertrack.android.use_case.deeplink.LoginWithDeeplinkParamsUseCase
import com.hypertrack.android.use_case.login.SignInUseCase
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class SignInViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
    private val signInUseCase: SignInUseCase,
    private val loginWithDeeplinkParamsUseCase: LoginWithDeeplinkParamsUseCase,
    private val loadUserStateAfterSignInUseCase: LoadUserStateAfterSignInUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val branchWrapper: BranchWrapper,
    private val moshi: Moshi,
) : BaseViewModel(baseDependencies) {

    private val signInWithCognitoUseCase = SignInWithCognitoUseCase(
        signInUseCase, resourceProvider
    )
    private val confirmationRequiredUseCase = ConfirmationRequiredUseCase(destination)
    private val handlePastedDeeplinkOrTokenUseCase = HandlePastedDeeplinkOrTokenUseCase(
        loginWithDeeplinkParamsUseCase,
        logMessageToCrashlyticsUseCase,
        logExceptionToCrashlyticsUseCase,
        branchWrapper,
        osUtilsProvider,
        resourceProvider,
        moshi
    )
    private val handleSignInUseCase = HandleSignInUseCase(appInteractor, destination)
    private val handleDeeplinkFailureUseCase = HandleDeeplinkFailureUseCase(resourceProvider)

    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        State(
            login = "",
            password = "",
            showPasteDeeplinkDialog = false
        ),
        viewModelScope,
        Dispatchers.Main,
        this::reduce,
        this::applyEffects,
        this::stateChangeEffects,
    )

    val clearDeeplinkTextEvent = MutableLiveData<Consumable<Unit>>()
    val errorMessageEvent = MutableLiveData<Consumable<ErrorMessage>>()

    val showProgressbar = MediatorLiveData<Boolean>().apply {
        addSource(appInteractor.appState) {
            Log.v("progress", "source $it")
            postValue(it.isProgressbarVisible())
        }
        addSource(loadingState) {
            Log.v("progress", "source $it")
            postValue(
                if (appInteractor.appState.requireValue().isProgressbarVisible()) {
                    true
                } else {
                    loadingState.requireValue()
                }
            )
        }
    }
    val viewState = MutableLiveData<ViewState>()

    fun handleAction(action: Action) {
        stateMachine.handleAction(action)
    }

    private fun reduce(state: State, action: Action): ReducerResult<State, Effect> {
        return when (action) {
            is LoginChangedAction -> {
                state.copy(login = action.login).withEffects()
            }
            is PasswordChangedAction -> {
                state.copy(password = action.password).withEffects()
            }
            OnLoginClickAction -> {
                state.withEffects(
                    SignInEffect(
                        login = state.login,
                        password = state.password
                    )
                )
            }
            is DeeplinkOrTokenPastedAction -> {
                state.withEffects(HandleDeeplinkOrTokenEffect(action.text, action.activity))
            }
            OnDeeplinkIssuesClickAction -> {
                state.copy(showPasteDeeplinkDialog = true).withEffects()
            }
            OnCloseClickAction -> {
                state.copy(showPasteDeeplinkDialog = false).withEffects(
                    ClearDeeplinkTextEffect
                )
            }
            is ErrorAction -> {
                state.withEffects(ErrorEffect(action.exception))
            }
        }
    }

    private fun applyEffects(effects: Set<Effect>) {
        runInVmEffectsScope {
            effects.forEach {
                getEffectFlow(it)
                    .catchException { e ->
                        crashReportsProvider.logException(e)
                        errorMessageEvent.postValue(ErrorMessage(e))
                    }
                    .collect()
            }
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<Unit> {
        return when (effect) {
            is SignInEffect -> {
                signInFlow(effect)
            }
            is UpdateViewStateEffect -> {
                { viewState.postValue(effect.viewState) }.asFlow()
            }
            is HandleDeeplinkOrTokenEffect -> {
                pasteDeeplinkOrTokenFlow(effect.text, effect.activity)
            }
            is ErrorEffect -> {
                handleErrorFlow(JustFailure(effect.exception))
            }
            ClearDeeplinkTextEffect -> {
                { clearDeeplinkTextEvent.postValue(Unit) }.asFlow()
            }
        }
    }

    private fun stateChangeEffects(state: State): Set<Effect> {
        return setOf(
            UpdateViewStateEffect(
                ViewState(
                    isLoginButtonEnabled = state.login.isNotBlank()
                            && state.password.isNotBlank(),
                    showPasteDeeplink = showProgressbar.requireValue().let {
                        if (it) {
                            false
                        } else {
                            state.showPasteDeeplinkDialog
                        }
                    }
                )
            )
        )
    }

    private fun signInFlow(effect: SignInEffect): Flow<Unit> {
        return loadingStateFlow(true)
            .flatMapConcat {
                signInWithCognitoUseCase.execute(
                    login = effect.login,
                    password = effect.password
                )
            }
            .flatMapSimpleSuccess {
                when (it) {
                    is SignInSuccess -> {
                        loadUserStateAfterSignInUseCase
                            .execute(it.loggedIn).flowOn(Dispatchers.Main)
                            .flatMapSimpleSuccess {
                                handleSignInUseCase.execute(it)
                            }
                    }
                    ConfirmationRequired -> {
                        confirmationRequiredUseCase.execute(effect.login)
                    }
                }
            }
            .flatMapConcat {
                handleErrorFlow(it)
            }
            .flatMapConcat {
                loadingStateFlow(false)
            }
    }

    private fun pasteDeeplinkOrTokenFlow(text: String, activity: Activity): Flow<Unit> {
        return loadingStateFlow(true)
            .flatMapConcat {
                handlePastedDeeplinkOrTokenUseCase.execute(text, activity)
            }
            .flatMapAbstractSuccess { it: LoggedIn ->
                loadUserStateAfterSignInUseCase
                    .execute(it).flowOn(Dispatchers.Main)
                    .map { result ->
                        when (result) {
                            is Success -> {
                                AbstractSuccess(result.data)
                            }
                            is Failure -> {
                                AbstractFailure(
                                    DeeplinkValidationError(
                                        DeeplinkException(result.exception)
                                    )
                                )
                            }
                        }
                    }
            }
            .flatMapConcat {
                when (it) {
                    is AbstractSuccess -> {
                        handleSignInUseCase.execute(it.success)
                    }
                    is AbstractFailure -> {
                        handleDeeplinkFailureUseCase.execute(it.failure.failure)
                    }
                }
            }
            .flatMapConcat {
                handleErrorFlow(it)
            }
            .flatMapConcat {
                loadingStateFlow(false)
            }
    }

    private fun handleErrorFlow(result: SimpleResult): Flow<Unit> {
        return when (result) {
            JustSuccess -> {
                flowOf(Unit)
            }
            is JustFailure -> {
                {
                    crashReportsProvider.logException(result.exception)
                    errorMessageEvent.postValue(mapUiErrorMessage(result.exception))
                }.asFlow()
            }
        }
    }

    private fun loadingStateFlow(isLoading: Boolean): Flow<Unit> {
        return {
            Log.v("progress", "post value $isLoading")
            loadingState.postValue(isLoading)
        }.asFlow()
    }

    private fun mapUiErrorMessage(exception: Exception): ErrorMessage {
        return if (MyApplication.DEBUG_MODE) {
            return ErrorMessage(exception)
        } else {
            when (exception) {
                is SimpleException -> ErrorMessage(exception)
                else -> ErrorMessage(resourceProvider.stringFromResource(R.string.unknown_error))
            }
        }
    }

}
