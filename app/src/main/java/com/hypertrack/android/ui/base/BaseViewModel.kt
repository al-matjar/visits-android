package com.hypertrack.android.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.common.use_case.ShowErrorUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.use_case.get_error_message.TextError
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.use_case.app.threading.ActionsScope
import com.hypertrack.android.use_case.app.threading.EffectsScope
import com.hypertrack.android.utils.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("LeakingThis", "OPT_IN_USAGE")
open class BaseViewModel(
    private val baseDependencies: BaseViewModelDependencies
) : ViewModel() {

    protected val crashReportsProvider = baseDependencies.crashReportsProvider
    protected val appInteractor = baseDependencies.appInteractor
    protected val osUtilsProvider = baseDependencies.osUtilsProvider
    protected val resourceProvider = baseDependencies.resourceProvider
    protected val actionsScope = baseDependencies.actionsScope
    protected val effectsScope = baseDependencies.effectsScope

    val destination = MutableLiveData<Consumable<NavDirections>>()
    val popBackStack = MutableLiveData<Consumable<Boolean>>()
    val snackbar = MutableLiveData<Consumable<String>>()
    val showErrorMessageEvent = MutableLiveData<Consumable<ErrorMessage>>()
    open val loadingState = MutableLiveData<Boolean>()

    protected val getErrorMessageUseCase = GetErrorMessageUseCase(resourceProvider)
    protected val showErrorUseCase = ShowErrorUseCase(
        showErrorMessageEvent,
        getErrorMessageUseCase
    )

    private val liveDataObserverManager = LiveDataObserverManager()

    fun withLoadingStateAndErrorHandler(code: (suspend () -> Unit)) {
        loadingState.postValue(true)
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            if (e is Exception) {
                showExceptionMessageAndReport(e)
                loadingState.postValue(false)
            } else {
                crashReportsProvider.logException(e)
            }
        }) {
            code.invoke()
            loadingState.postValue(false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCleared() {
        liveDataObserverManager.onCleared()
    }

    open fun onError(exception: Exception) {
        // todo avoid infinite loop on error when reporting error
        showExceptionMessageAndReport(exception)
    }

    protected fun showErrorAndReportFlow(exception: Exception): Flow<Unit> {
        return { showExceptionMessageAndReport(exception) }.asFlow()
    }

    protected fun showExceptionMessageAndReport(consumable: Consumable<Exception>) {
        consumable.consume {
            showExceptionMessageAndReport(it)
        }
    }

    // todo remove workaround for legacy code (just use flow for effect)
    protected fun showExceptionMessageAndReport(exception: Exception) {
        runInVmEffectsScope {
            crashReportsProvider.logException(exception)
            showErrorUseCase.execute(ExceptionError(exception)).collect()
        }
    }

    // todo remove workaround for legacy code (just use flow for effect)
    protected fun showError(@StringRes stringRes: Int) {
        runInVmEffectsScope {
            showErrorUseCase.execute(TextError(stringRes)).collect()
        }
    }

    protected fun <T> LiveData<T>.observeManaged(observer: Observer<T>) {
        this.observeManaged(liveDataObserverManager, observer)
    }

    protected fun runInVmEffectsScope(block: suspend CoroutineScope.() -> Unit): Job {
        // todo sync with splash screen and activity viewmodel
        return viewModelScope.launch(Dispatchers.Default, block = block)
    }

    fun showErrorFlow(displayableError: DisplayableError): Flow<Unit> {
        return showErrorUseCase.execute(displayableError)
    }

    fun Flow<DisplayableError>.showErrorMessage(): Flow<Unit> {
        return flatMapConcat {
            showErrorUseCase.execute(it)
        }
    }

    fun Flow<DisplayableError>.showErrorAndReport(): Flow<Unit> {
        return flatMapConcat {
            showErrorUseCase.execute(it)
        }
    }
}

class BaseViewModelDependencies(
    val appInteractor: AppInteractor,
    val osUtilsProvider: OsUtilsProvider,
    val resourceProvider: ResourceProvider,
    val crashReportsProvider: CrashReportsProvider,
    val actionsScope: ActionsScope,
    val effectsScope: EffectsScope
)

fun NavController.navigate(d: Consumable<NavDirections>) {
    d.consume {
        navigate(it)
    }
}

fun withErrorHandling(
    errorHandler: (Exception) -> Unit,
    block: () -> Unit
) {
    try {
        block.invoke()
    } catch (e: Exception) {
        errorHandler.invoke(e)
    }
}


