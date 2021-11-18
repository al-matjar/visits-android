package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.delegates.DeeplinkResultDelegate
import com.hypertrack.android.interactors.*
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class SignInViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val loginInteractor: LoginInteractor,
    private val permissionsInteractor: PermissionsInteractor,
    private val deeplinkInteractor: DeeplinkInteractor,
    private val deeplinkProcessor: DeeplinkProcessor,
    private val moshi: Moshi,
) : BaseViewModel(baseDependencies) {

    private var login = ""
    private var password = ""

    val isLoginButtonClickable = MutableLiveData(false)
    val loginErrorText = MutableLiveData<String>()

    val deeplinkErrorText = MutableLiveData<String?>()
    val clearDeeplinkTextAction = MutableLiveData<Consumable<Boolean>>()
    val showPasteDeeplink = MutableLiveData<Boolean>(false)


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
        loginErrorText.postValue("")
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
                            loginErrorText.postValue(osUtilsProvider.stringFromResource(R.string.user_does_not_exist))
                        }
                        is InvalidLoginOrPassword -> {
                            loginErrorText.postValue(osUtilsProvider.stringFromResource(R.string.incorrect_username_or_pass))
                        }
                        is EmailConfirmationRequired -> {
                            destination.postValue(
                                SignInFragmentDirections.actionSignInFragmentToConfirmFragment(
                                    login
                                )
                            )
                        }
                        is LoginError -> {
                            loginErrorText.postValue(MyApplication.context.getString(R.string.unknown_error))
                        }
                        is PublishableKey -> throw IllegalStateException()
                    }
                }
            }
        }
    }

    fun handleDeeplinkOrToken(text: String, activity: Activity) {
        deeplinkErrorText.postValue(null)
        with(DeeplinkProcessor.DEEPLINK_REGEX.matcher(text)) {
            if (matches()) {
                handleDeeplink(text, activity)
            } else {
                handleLoginToken(text, activity)
            }
        }
    }

    fun onDeeplinkIssuesClick() {
        showPasteDeeplink.postValue(true)
        clearDeeplinkTextAction.postValue(true.toConsumable())
        deeplinkErrorText.postValue(null)
    }

    fun onCloseClick() {
        showPasteDeeplink.postValue(false)
    }

    private fun handleLoginToken(loginToken: String, activity: Activity) {
        try {
            osUtilsProvider.decodeBase64(
                if (loginToken.contains("?")) {
                    loginToken.split("?")[0]
                } else {
                    loginToken
                }
            ).let { json ->
                moshi.createAnyMapAdapter().fromJson(json)!!.let {
                    loadingState.postValue(true)
                    onDeeplinkParamsReceived(
                        DeeplinkParams(it["data"] as Map<String, Any>),
                        activity
                    )
                }
            }

        } catch (e: Exception) {
            crashReportsProvider.logException(e, mapOf("loginToken" to loginToken))
            val message =
                resourceProvider.stringFromResource(R.string.sign_in_deeplink_invalid_format)
            deeplinkErrorText.postValue("$message\n\n${e.format()}")
        }
    }

    protected open fun onDeeplinkParamsReceived(
        deeplinkResult: DeeplinkResult,
        activity: Activity
    ) {
        viewModelScope.launch {
            deeplinkDelegate.handleDeeplink(
                deeplinkResult,
                activity
            )
        }
    }

    private fun handleDeeplink(link: String, activity: Activity) {
        loadingState.postValue(true)
        viewModelScope.launch {
            val result = suspendCoroutine<DeeplinkResult> {
                deeplinkProcessor.onLinkRetrieved(activity, link, object : DeeplinkResultListener {
                    override fun onDeeplinkResult(result: DeeplinkResult) {
                        it.resume(result)
                    }
                })
            }
            onDeeplinkParamsReceived(result, activity)
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