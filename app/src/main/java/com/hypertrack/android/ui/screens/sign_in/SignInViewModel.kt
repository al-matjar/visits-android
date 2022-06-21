package com.hypertrack.android.ui.screens.sign_in

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.deeplink.BranchWrapper
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.sign_in.use_case.ConfirmationRequired
import com.hypertrack.android.ui.screens.sign_in.use_case.ConfirmationRequiredUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandleDeeplinkFailureUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandlePastedDeeplinkOrTokenUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.HandleSignInUseCase
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInInvalidLoginOrPassword
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInNoSuchUser
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInSuccess
import com.hypertrack.android.ui.screens.sign_in.use_case.SignInWithCognitoUseCase
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.DeeplinkException
import com.hypertrack.android.use_case.deeplink.DeeplinkValidationError
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
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
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase,
    private val loginWithDeeplinkParamsUseCase: LoginWithDeeplinkParamsUseCase,
    private val loadUserStateAfterSignInUseCase: LoadUserStateAfterSignInUseCase,
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val branchWrapper: BranchWrapper,
    private val moshi: Moshi
) : BaseViewModel(baseDependencies) {

    private val signInWithCognitoUseCase = SignInWithCognitoUseCase(
        signInUseCase, resourceProvider
    )
    private val confirmationRequiredUseCase = ConfirmationRequiredUseCase(destination)
    private val handlePastedDeeplinkOrTokenUseCase = HandlePastedDeeplinkOrTokenUseCase(
        getBranchDataFromAppBackendUseCase,
        loginWithDeeplinkParamsUseCase,
        logMessageToCrashlyticsUseCase,
        logExceptionToCrashlyticsUseCase,
        branchWrapper,
        osUtilsProvider,
        moshi
    )
    private val handleSignInUseCase = HandleSignInUseCase(appInteractor, destination)
    private val handleDeeplinkFailureUseCase = HandleDeeplinkFailureUseCase(
        logExceptionToCrashlyticsUseCase,
        showErrorUseCase
    )

    private val stateMachine = StateMachine<Action, State, Effect>(
        javaClass.simpleName,
        crashReportsProvider,
        State(
            login = "",
            password = "",
            deeplinkIssuesDialog = Hidden
        ),
        viewModelScope,
        Dispatchers.Main,
        this::reduce,
        this::applyEffects,
        this::stateChangeEffects,
    )

    val clearDeeplinkTextEvent = MutableLiveData<Consumable<Unit>>()

    val showProgressbar = MediatorLiveData<Boolean>().apply {
        postValue(false)
        addSource(appInteractor.appState) {
            postValue(it.isProgressbarVisible())
        }
        addSource(loadingState) {
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

    fun onDeeplinkIssuesClicked() {
        applyEffect(PrepareOnDeeplinkIssuesClickedActionEffect)
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
            is OnDeeplinkIssuesClickAction -> {
                state.copy(deeplinkIssuesDialog = Displayed(action.hardwareId)).withEffects()
            }
            OnCloseClickAction -> {
                state.copy(deeplinkIssuesDialog = Hidden).withEffects(
                    ClearDeeplinkTextEffect
                )
            }
            is ErrorAction -> {
                state.withEffects(ErrorEffect(action.exception))
            }
            CopyHardwareIdAction -> {
                when (state.deeplinkIssuesDialog) {
                    is Displayed -> state.withEffects(
                        CopyHardwareIdEffect(
                            state.deeplinkIssuesDialog.hardwareId
                        )
                    )
                    Hidden -> state.withEffects()
                }
            }
        }
    }

    private fun applyEffects(effects: Set<Effect>) {
        effects.forEach {
            applyEffect(it)
        }
    }

    private fun applyEffect(effect: Effect) {
        runInVmEffectsScope {
            crashReportsProvider.log("Running effect: $effect")
            getEffectFlow(effect)
                .map { action ->
                    action?.let { handleAction(action) }
                }
                .catchException { e ->
                    showExceptionMessageAndReport(e)
                }
                .collect()
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<Action?> {
        return when (effect) {
            is SignInEffect -> {
                signInFlow(effect).map { null }
            }
            is UpdateViewStateEffect -> {
                { viewState.postValue(effect.viewState) }.asFlow().map { null }
            }
            is HandleDeeplinkOrTokenEffect -> {
                pasteDeeplinkOrTokenFlow(effect.text).map { null }
            }
            is ErrorEffect -> {
                { showExceptionMessageAndReport(effect.exception) }.asFlow().map { null }
            }
            ClearDeeplinkTextEffect -> {
                { clearDeeplinkTextEvent.postValue(Unit) }.asFlow().map { null }
            }
            is CopyHardwareIdEffect -> {
                { osUtilsProvider.copyToClipboard(effect.hardwareId.value) }.asFlow().map { null }
                    .flowOn(Dispatchers.Main)
            }
            is PrepareOnDeeplinkIssuesClickedActionEffect -> {
                { DeviceInfoUtils.getHardwareId(appInteractor.appScope.appContext) }.asFlow()
                    .map {
                        when (it) {
                            is Success -> OnDeeplinkIssuesClickAction(it.data)
                            is Failure -> ErrorAction(it.exception)
                        }
                    }
            }
        }
    }

    private fun stateChangeEffects(state: State): Set<Effect> {
        return setOf(
            UpdateViewStateEffect(
                ViewState(
                    isLoginButtonEnabled = state.login.isNotBlank()
                            && state.password.isNotBlank(),
                    showDeeplinkIssuesDialog = showProgressbar.requireValue()
                        .let { showProgressbar ->
                            if (showProgressbar) {
                                false
                            } else {
                                when (state.deeplinkIssuesDialog) {
                                    is Displayed -> true
                                    Hidden -> false
                                }
                            }
                        },
                    hardwareId = when (state.deeplinkIssuesDialog) {
                        is Displayed -> state.deeplinkIssuesDialog.hardwareId
                        Hidden -> null
                    }
                )
            )
        )
    }

    private fun signInFlow(effect: SignInEffect): Flow<Unit> {
        return startLoading()
            .flatMapConcat {
                signInWithCognitoUseCase.execute(
                    login = effect.login.trim(),
                    password = effect.password
                )
            }.flatMapConcat { result ->
                when (result) {
                    is Success -> {
                        when (val signInResult = result.data) {
                            is SignInSuccess -> {
                                loadUserStateAfterSignInUseCase
                                    .execute(signInResult.loggedIn).flowOn(Dispatchers.Main)
                                    .flatMapSimpleSuccess {
                                        handleSignInUseCase.execute(it)
                                    }.showErrorAndReportIfFailure()
                            }
                            ConfirmationRequired -> {
                                confirmationRequiredUseCase.execute(effect.login)
                                    .showErrorAndReportIfFailure()
                            }
                            SignInInvalidLoginOrPassword -> {
                                showErrorUseCase.execute(
                                    R.string.incorrect_username_or_pass.asError()
                                )
                            }
                            SignInNoSuchUser -> {
                                showErrorUseCase.execute(R.string.user_does_not_exist.asError())
                            }
                        }
                    }
                    is Failure -> {
                        showErrorAndReportFlow(result.exception)
                    }
                }
            }
            .stopLoading()
    }

    private fun pasteDeeplinkOrTokenFlow(text: String): Flow<Unit> {
        return startLoading()
            .flatMapConcat {
                handlePastedDeeplinkOrTokenUseCase.execute(text)
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
                        handleDeeplinkFailureUseCase.execute(it.failure.failure).map {
                            JustSuccess
                        }
                    }
                }
            }
            .showErrorAndReportIfFailure()
            .stopLoading()
    }

    private fun Flow<SimpleResult>.showErrorAndReportIfFailure(): Flow<Unit> {
        return flatMapConcat { result ->
            when (result) {
                JustSuccess -> {
                    flowOf(Unit)
                }
                is JustFailure -> {
                    { showExceptionMessageAndReport(result.exception) }.asFlow()
                }
            }
        }
    }

    private fun Flow<Unit>.stopLoading(): Flow<Unit> {
        return flatMapConcat {
            { loadingState.postValue(false) }.asFlow()
        }
    }

    private fun startLoading(): Flow<Unit> {
        return { loadingState.postValue(true) }.asFlow()
    }

}
