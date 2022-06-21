package com.hypertrack.android.ui.screens.confirm_email

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.interactors.*
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.SignedInAction
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.use_case.login.LoadUserStateAfterSignInUseCase
import com.hypertrack.android.use_case.login.OtpError
import com.hypertrack.android.use_case.login.OtpFailure
import com.hypertrack.android.use_case.login.OtpSignInRequired
import com.hypertrack.android.use_case.login.OtpWrongCode
import com.hypertrack.android.use_case.login.ResendAlreadyConfirmed
import com.hypertrack.android.use_case.login.ResendEmailConfirmationUseCase
import com.hypertrack.android.use_case.login.ResendError
import com.hypertrack.android.use_case.login.ResendNoAction
import com.hypertrack.android.use_case.login.VerifyByOtpCodeUseCase
import com.hypertrack.android.utils.AbstractFailure
import com.hypertrack.android.utils.AbstractSuccess
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class ConfirmEmailViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appInteractor: AppInteractor,
    private val verifyByOtpCodeUseCase: VerifyByOtpCodeUseCase,
    private val resendEmailConfirmationUseCase: ResendEmailConfirmationUseCase,
    private val loadUserStateAfterSignInUseCase: LoadUserStateAfterSignInUseCase,
) : BaseViewModel(baseDependencies) {

    private lateinit var email: String

    val proceedButtonEnabled = MutableLiveData<Boolean>(false)
    val clipboardCode = SingleLiveEvent<String>()

    fun init(email: String) {
        this.email = email
    }

    fun onClipboardReady() {
        osUtilsProvider.getClipboardContents()?.let {
            if (it.matches(Regex("^[0-9]{6}\$"))) {
                clipboardCode.postValue(it)
            }
        }
    }

    fun onVerifiedClick(code: String, complete: Boolean, activity: Activity) {
        if (complete) {
            loadingState.postValue(true)
            runInVmEffectsScope {
                verifyByOtpCodeUseCase.execute(email = email, code = code)
                    .flatMapConcat {
                        when (it) {
                            is AbstractSuccess -> {
                                loadUserStateAfterSignInUseCase
                                    .execute(it.success).flowOn(Dispatchers.Main)
                                    .map { result ->
                                        when (result) {
                                            is Success -> {
                                                AbstractSuccess(result.data)
                                            }
                                            is Failure -> {
                                                AbstractFailure<UserLoggedIn, OtpFailure>(
                                                    OtpError(
                                                        result.exception
                                                    )
                                                )
                                            }
                                        }

                                    }
                            }
                            is AbstractFailure -> {
                                flowOf(AbstractFailure<UserLoggedIn, OtpFailure>(it.failure))
                            }
                        }
                    }
                    .collect { res ->
                        loadingState.postValue(false)
                        when (res) {
                            is AbstractSuccess -> {
                                appInteractor.handleAction(SignedInAction(res.success))
                                res.success.userScope.permissionsInteractor.let { permissionsInteractor ->
                                    when (permissionsInteractor.checkPermissionsState()
                                        .getNextPermissionRequest()) {
                                        PermissionDestination.PASS -> {
                                            destination.postValue(ConfirmFragmentDirections.actionGlobalVisitManagementFragment())
                                        }
                                        PermissionDestination.FOREGROUND_AND_TRACKING -> {
                                            destination.postValue(ConfirmFragmentDirections.actionGlobalPermissionRequestFragment())
                                        }
                                        PermissionDestination.BACKGROUND -> {
                                            destination.postValue(ConfirmFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                                        }
                                    }
                                }
                            }
                            is AbstractFailure -> {
                                when (res.failure) {
                                    is OtpSignInRequired -> {
                                        destination.postValue(
                                            ConfirmFragmentDirections
                                                .actionConfirmFragmentToSignInFragment(
                                                    email
                                                )
                                        )
                                    }
                                    is OtpWrongCode -> {
                                        showError(R.string.wrong_code)
                                    }
                                    is OtpError -> {
                                        showExceptionMessageAndReport(res.failure.exception)
                                    }
                                }
                            }
                        } as Any?
                    }
            }
        }
    }

    fun onResendClick() {
        loadingState.postValue(true)
        runInVmEffectsScope {
            resendEmailConfirmationUseCase.execute(email).collect { res ->
                loadingState.postValue(false)
                when (res) {
                    ResendNoAction -> {
                    }
                    ResendAlreadyConfirmed -> {
                        destination.postValue(
                            ConfirmFragmentDirections.actionConfirmFragmentToSignInFragment(
                                email
                            )
                        )
                    }
                    is ResendError -> {
                        showExceptionMessageAndReport(res.exception)
                    }
                }
            }
        }
    }

    fun onCodeChanged(code: String, complete: Boolean, activity: Activity) {
        if (complete) {
            onVerifiedClick(code, complete, activity)
        }
        proceedButtonEnabled.postValue(complete)
    }

}
