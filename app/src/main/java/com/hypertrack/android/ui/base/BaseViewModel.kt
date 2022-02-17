package com.hypertrack.android.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.hypertrack.android.utils.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Suppress("LeakingThis")
open class BaseViewModel(
    private val baseDependencies: BaseViewModelDependencies
) : ViewModel() {
    protected val crashReportsProvider = baseDependencies.crashReportsProvider
    protected val osUtilsProvider = baseDependencies.osUtilsProvider
    protected val resourceProvider = baseDependencies.resourceProvider

    val destination = MutableLiveData<Consumable<NavDirections>>()
    val popBackStack = MutableLiveData<Consumable<Boolean>>()
    val snackbar = MutableLiveData<Consumable<String>>()

    open val errorHandler =
        ErrorHandler(baseDependencies.osUtilsProvider, baseDependencies.crashReportsProvider)

    open val loadingState = MutableLiveData<Boolean>()

    private val liveDataObserverManager = LiveDataObserverManager()

    protected fun <T> LiveData<T>.observeManaged(observer: Observer<T>) {
        this.observeManaged(liveDataObserverManager, observer)
    }

    fun withLoadingStateAndErrorHandler(code: (suspend () -> Unit)) {
        loadingState.postValue(true)
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            if (e is Exception) {
                errorHandler.postException(e)
                loadingState.postValue(false)
            } else {
                crashReportsProvider.logException(e)
            }
        }) {
            code.invoke()
            loadingState.postValue(false)
        }
    }

    protected fun runInVmEffectsScope(block: suspend CoroutineScope.() -> Unit): Job {
        // todo sync with splash screen and activity viewmodel
        return viewModelScope.launch(Dispatchers.Default, block = block)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCleared() {
        liveDataObserverManager.onCleared()
    }
}

class BaseViewModelDependencies(
    val osUtilsProvider: OsUtilsProvider,
    val resourceProvider: ResourceProvider,
    val crashReportsProvider: CrashReportsProvider,
)

class ErrorHandler(
    private val resourceProvider: ResourceProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val exceptionSource: LiveData<Consumable<Exception>>? = null,
    private val errorTextSource: LiveData<Consumable<String>>? = null
) {

    val exception: LiveData<Consumable<Exception>>
        get() = _exception
    private val _exception = MediatorLiveData<Consumable<Exception>>().apply {
        exceptionSource?.let {
            addSource(exceptionSource) {
                onExceptionReceived(it.payload)
                postValue(it)
            }
        }
    }

    val errorText: LiveData<Consumable<String>>
        get() = _errorText
    private val _errorText = MediatorLiveData<Consumable<String>>().apply {
        errorTextSource?.let {
            addSource(errorTextSource) {
                postValue(it)
            }
        }
        addSource(_exception) {
            postValue(it.map {
                resourceProvider.getErrorMessage(it).text
            })
        }
    }

    fun postConsumable(e: Consumable<Exception>) {
        onExceptionReceived(e.payload)
        _exception.postValue(e)
    }

    fun postException(e: Exception) {
        onExceptionReceived(e)
        _exception.postValue(Consumable(e))
    }

    fun postText(e: String) {
        _errorText.postValue(Consumable(e))
    }

    fun postText(@StringRes res: Int) {
        _errorText.postValue(Consumable(resourceProvider.stringFromResource(res)))
    }

    private fun onExceptionReceived(e: Exception) {
        crashReportsProvider.logException(e)
    }

    fun handle(code: () -> Unit) {
        try {
            code.invoke()
        } catch (e: Exception) {
            postException(e)
        }
    }

}

fun <T> MutableLiveData<Consumable<T>>.postValue(item: T) {
    postValue(Consumable(item))
}

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
