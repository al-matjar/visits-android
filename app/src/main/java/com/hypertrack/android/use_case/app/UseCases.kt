package com.hypertrack.android.use_case.app

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.use_case.login.GetPublishableKeyWithCognitoUseCase
import com.hypertrack.android.use_case.login.LoadUserStateAfterSignInUseCase
import com.hypertrack.android.use_case.login.LoadUserStateUseCase
import com.hypertrack.android.use_case.deeplink.LoginWithDeeplinkParamsUseCase
import com.hypertrack.android.use_case.navigation.NavigateToUserScopeScreensUseCase
import com.hypertrack.android.use_case.login.LoginWithPublishableKeyUseCase
import com.hypertrack.android.use_case.login.RefreshUserAccessTokenUseCase
import com.hypertrack.android.use_case.login.ResendEmailConfirmationUseCase
import com.hypertrack.android.use_case.login.SignInUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkParamsUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase.Companion.createDeeplinkRegex
import com.hypertrack.android.use_case.error.LogExceptionIfFailureUseCase
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.use_case.login.DeleteUserScopeDataUseCase
import com.hypertrack.android.use_case.login.DeleteUserScopeUseCase
import com.hypertrack.android.use_case.login.LoadUserDataUseCase
import com.hypertrack.android.use_case.login.VerifyByOtpCodeUseCase
import com.hypertrack.android.use_case.sdk.GetConfiguredHypertrackSdkInstanceUseCase
import com.hypertrack.android.use_case.sdk.GetHypertrackSdkInstanceUseCase

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class UseCases(
    private val appScope: AppScope,
    private val appInteractor: AppInteractor,
    private val servicesApiKey: String,
) {
    private val createUserScopeUseCase = CreateUserScopeUseCase(appScope, appInteractor)

    private val deleteUserScopeDataUseCase = DeleteUserScopeDataUseCase(
        appScope.userRepository,
        appScope.publishableKeyRepository,
        appScope.measurementUnitsRepository,
        appScope.accessTokenRepository,
        appScope.myPreferences,
    )

    private val loadUserDataUseCase = LoadUserDataUseCase(
        appScope.userRepository,
        deleteUserScopeDataUseCase
    )

    val setCrashReportingIdUseCase = SetCrashReportingIdUseCase(
        appScope.crashReportsProvider,
        appScope.moshi
    )

    val loadUserStateUseCase = LoadUserStateUseCase(
        appScope.appContext,
        createUserScopeUseCase,
        setCrashReportingIdUseCase,
        loadUserDataUseCase
    )

    val loadUserStateAfterSignInUseCase = LoadUserStateAfterSignInUseCase(
        loadUserStateUseCase
    )

    val logExceptionToCrashlyticsUseCase = LogExceptionToCrashlyticsUseCase(
        appScope.crashReportsProvider
    )

    val logExceptionIfFailureUseCase = LogExceptionIfFailureUseCase(
        logExceptionToCrashlyticsUseCase
    )

    val logMessageToCrashlyticsUseCase = LogMessageToCrashlyticsUseCase(
        appScope.crashReportsProvider
    )

    private val getHypertrackSdkInstanceUseCase = GetHypertrackSdkInstanceUseCase()

    val getConfiguredHypertrackSdkInstanceUseCase = GetConfiguredHypertrackSdkInstanceUseCase(
        appScope.trackingStateListener,
        getHypertrackSdkInstanceUseCase
    )

    val loginWithPublishableKeyUseCase = LoginWithPublishableKeyUseCase(
        RefreshUserAccessTokenUseCase(appScope.accessTokenRepository),
        appScope.userRepository,
        appScope.accessTokenRepository,
        appScope.publishableKeyRepository,
    )

    val loginWithDeeplinkParamsUseCase = LoginWithDeeplinkParamsUseCase(
        ValidateDeeplinkParamsUseCase(appScope.moshi)
    )

    val signInUseCase = SignInUseCase(
        GetPublishableKeyWithCognitoUseCase(
            appScope.cognitoAccountLoginProvider,
            appScope.tokenApi,
        )
    )

    val resendExceptionToCrashlyticsUseCase = ResendEmailConfirmationUseCase(
        appScope.liveAccountApi,
        servicesApiKey
    )

    val verifyByOtpCodeUseCase = VerifyByOtpCodeUseCase(
        appScope.liveAccountApi,
        servicesApiKey
    )

    val navigateToUserScopeScreensUseCase = NavigateToUserScopeScreensUseCase()

    val getErrorMessageUseCase = GetErrorMessageUseCase(appScope.resourceProvider)

    val validateDeeplinkUrlUseCase = ValidateDeeplinkUrlUseCase(createDeeplinkRegex())

    val getBranchDataFromAppBackendUseCase = GetBranchDataFromAppBackendUseCase(
        appInteractor.appScope.appBackendApi,
        logMessageToCrashlyticsUseCase,
        appScope.moshi
    )

    val destroyUserScopeUseCase = DeleteUserScopeUseCase(deleteUserScopeDataUseCase)

    override fun toString(): String = javaClass.simpleName
}
