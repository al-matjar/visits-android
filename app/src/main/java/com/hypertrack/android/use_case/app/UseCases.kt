package com.hypertrack.android.use_case.app

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.use_case.login.GetPublishableKeyWithCognitoUseCase
import com.hypertrack.android.use_case.login.LoadUserStateAfterSignInUseCase
import com.hypertrack.android.use_case.login.LoadUserStateUseCase
import com.hypertrack.android.use_case.deeplink.LoginWithDeeplinkParamsUseCase
import com.hypertrack.android.use_case.navigation.NavigateToUserScopeScreensUseCase
import com.hypertrack.android.use_case.login.LoginWithPublishableKeyUseCase
import com.hypertrack.android.use_case.login.RefreshUserAccessTokenUseCase
import com.hypertrack.android.use_case.login.ResendEmailConfirmationUseCase
import com.hypertrack.android.use_case.login.SignInUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUseCase
import com.hypertrack.android.use_case.login.DeleteUserScopeDataUseCase
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

    val loadUserStateUseCase = LoadUserStateUseCase(
        createUserScopeUseCase,
        loadUserDataUseCase,
        appScope.trackingState
    )

    val loadUserStateAfterSignInUseCase = LoadUserStateAfterSignInUseCase(
        loadUserStateUseCase
    )

    val logExceptionToCrashlyticsUseCase = LogExceptionToCrashlyticsUseCase(
        appScope.crashReportsProvider
    )

    val logMessageToCrashlyticsUseCase = LogMessageToCrashlyticsUseCase(
        appScope.crashReportsProvider
    )

    val setCrashReportingIdUseCase = SetCrashReportingIdUseCase(
        appScope.crashReportsProvider,
        appScope.moshi
    )

    private val getHypertrackSdkInstanceUseCase = GetHypertrackSdkInstanceUseCase()

    val getConfiguredHypertrackSdkInstanceUseCase = GetConfiguredHypertrackSdkInstanceUseCase(
        appScope.trackingStateListener,
        getHypertrackSdkInstanceUseCase
    )

    private val loginWithPublishableKeyUseCase = LoginWithPublishableKeyUseCase(
        RefreshUserAccessTokenUseCase(appScope.accessTokenRepository),
        appScope.userRepository,
        appScope.accessTokenRepository,
        appScope.publishableKeyRepository,
    )

    val loginWithDeeplinkUseCase = LoginWithDeeplinkParamsUseCase(
        ValidateDeeplinkUseCase(appScope.moshi),
        getConfiguredHypertrackSdkInstanceUseCase,
        loginWithPublishableKeyUseCase,
        logExceptionToCrashlyticsUseCase,
    )

    val signInUseCase = SignInUseCase(
        GetPublishableKeyWithCognitoUseCase(
            appScope.cognitoAccountLoginProvider,
            appScope.tokenService,
        ),
        loginWithPublishableKeyUseCase,
        getConfiguredHypertrackSdkInstanceUseCase,
    )

    val resendExceptionToCrashlyticsUseCase = ResendEmailConfirmationUseCase(
        appScope.liveAccountApi,
        servicesApiKey
    )

    val verifyByOtpCodeUseCase = VerifyByOtpCodeUseCase(
        getConfiguredHypertrackSdkInstanceUseCase,
        loginWithPublishableKeyUseCase,
        appScope.liveAccountApi,
        servicesApiKey
    )

    val navigateToUserScopeScreensUseCase = NavigateToUserScopeScreensUseCase()

    override fun toString(): String = javaClass.simpleName
}
