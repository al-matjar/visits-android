package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.delegates.DeeplinkResultDelegate
import com.hypertrack.android.interactors.*
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignInViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val loginInteractor: LoginInteractor,
    private val permissionsInteractor: PermissionsInteractor,
    private val deeplinkInteractor: DeeplinkInteractor,
    private val deeplinkProcessor: DeeplinkProcessor,
) : BaseViewModel(baseDependencies) {

    private var login = ""
    private var password = ""

    val errorTextState = MutableLiveData<String>()
    val isLoginButtonClickable = MutableLiveData(false)

    val deeplinkErrorText = MutableLiveData<String?>()

    private val deeplinkDelegate = object : DeeplinkResultDelegate(
        deeplinkInteractor,
        crashReportsProvider,
        resourceProvider,
        errorHandler
    ) {
        override fun postError(text: String) {
            deeplinkErrorText.postValue(text)
        }

        override fun proceedToSignIn(handleDeeplinkResult: HandleDeeplinkResult) {
        }

        override fun proceedToVisitsManagement() {
            proceed()
        }
    }

    fun onLoginTextChanged(email: CharSequence) {
        login = email.toString()
        enableButtonIfInputNonEmpty()
    }

    fun onPasswordTextChanged(pwd: CharSequence) {
        password = pwd.toString()
        enableButtonIfInputNonEmpty()
    }

    fun onLoginClick() {
        errorTextState.postValue("")
        isLoginButtonClickable.postValue(false)
        loadingState.postValue(true)

        viewModelScope.launch {
            val res = loginInteractor.signIn(login, password)
            when (res) {
                is PublishableKey -> {
                    loadingState.postValue(false)
                    proceed()
                }
                else -> {
                    enableButtonIfInputNonEmpty()
                    loadingState.postValue(false)
                    when (res) {
                        is NoSuchUser -> {
                            errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.user_does_not_exist))
                        }
                        is InvalidLoginOrPassword -> {
                            errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.incorrect_username_or_pass))
                        }
                        is EmailConfirmationRequired -> {
                            destination.postValue(
                                SignInFragmentDirections.actionSignInFragmentToConfirmFragment(
                                    login
                                )
                            )
                        }
                        is LoginError -> {
                            errorTextState.postValue(MyApplication.context.getString(R.string.unknown_error))
                        }
                        is PublishableKey -> throw IllegalStateException()
                    }
                }
            }
        }
    }

    fun handleDeeplink(link: String, activity: Activity) {
        deeplinkErrorText.postValue(null)
        loadingState.postValue(true)
        viewModelScope.launch {
            val result = suspendCoroutine<DeeplinkResult> {
                deeplinkProcessor.onLinkRetrieved(activity, link, object : DeeplinkResultListener {
                    override fun onDeeplinkResult(result: DeeplinkResult) {
                        it.resume(result)
                    }
                })
            }
            deeplinkDelegate.handleDeeplink(result, activity)
            loadingState.postValue(false)
        }
    }

    private fun proceed() {
        when (permissionsInteractor.checkPermissionsState().getNextPermissionRequest()) {
            PermissionDestination.PASS -> {
                destination.postValue(SignInFragmentDirections.actionGlobalVisitManagementFragment())
            }
            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                destination.postValue(SignInFragmentDirections.actionGlobalPermissionRequestFragment())
            }
            PermissionDestination.BACKGROUND -> {
                destination.postValue(SignInFragmentDirections.actionGlobalBackgroundPermissionsFragment())
            }
        }
    }

    private fun enableButtonIfInputNonEmpty() {
        // Log.v(TAG, "enableButtonIfInputNonEmpty")
        if (login.isNotBlank() && password.isNotBlank()) {
            // Log.v(TAG, "enabling Button")
            isLoginButtonClickable.postValue(true)
        } else {
            isLoginButtonClickable.postValue(false)
        }
    }

}