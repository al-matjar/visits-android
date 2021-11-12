package com.hypertrack.android.ui.base

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import com.hypertrack.android.utils.*
import retrofit2.HttpException
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.CoroutineExceptionHandler
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

    open val errorHandler =
        ErrorHandler(baseDependencies.osUtilsProvider, baseDependencies.crashReportsProvider)

    open val loadingState = MutableLiveData<Boolean>()

    private val observers = mutableListOf<Pair<LiveData<*>, Observer<*>>>()

    protected fun <T> LiveData<T>.observeManaged(observer: Observer<T>) {
        observeForever(observer)
        observers.add(Pair(this, observer))
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

    @Suppress("UNCHECKED_CAST")
    override fun onCleared() {
        observers.forEach { it.first.removeObserver(it.second as Observer<Any>) }
    }
}

class BaseViewModelDependencies(
    val osUtilsProvider: OsUtilsProvider,
    val resourceProvider: ResourceProvider,
    val crashReportsProvider: CrashReportsProvider,
)

class ErrorHandler(
    private val osUtilsProvider: OsUtilsProvider,
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
                getErrorMessage(it)
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
        _errorText.postValue(Consumable(osUtilsProvider.stringFromResource(res)))
    }

    private fun onExceptionReceived(e: Exception) {
        crashReportsProvider.logException(e)
    }

    private fun getErrorMessage(e: Exception): String {
        //todo NonReportableException
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                if (MyApplication.DEBUG_MODE) {
                    Log.v("hypertrack-verbose", errorBody.toString())
                }
                val path = e.response()?.raw()?.request?.let {
                    "${it.method} ${e.response()!!.code()} ${it.url.encodedPath}"
                }
                return "${path.toString()}\n\n${errorBody.toString()}"
            }
            else -> {
                if (e.isNetworkError()) {
                    osUtilsProvider.stringFromResource(R.string.network_error)
                } else {
                    if (MyApplication.DEBUG_MODE) {
                        e.printStackTrace()
                    }
                    e.format()
                }
            }
        }
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