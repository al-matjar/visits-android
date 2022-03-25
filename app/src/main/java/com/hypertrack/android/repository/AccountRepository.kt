package com.hypertrack.android.repository

import com.hypertrack.android.data.AccountDataStorage
import com.hypertrack.android.utils.AccountPreferencesProvider
import com.hypertrack.android.utils.ServiceLocator
import kotlin.IllegalStateException

@Deprecated("needs refactoring")
class AccountRepository(
    private val serviceLocator: ServiceLocator,
    private val accountData: AccountData,
    private val accountDataStorage: AccountDataStorage,
    private val clearLoginAction: () -> Unit
) : AccountPreferencesProvider {

    val publishableKey: String
        get() {
            return accountDataStorage.getAccountData().publishableKey
                ?: throw IllegalStateException("No publishable key")
        }

    val isLoggedIn: Boolean
        get() = accountData.lastToken != null

    override var wasWhitelisted: Boolean
        get() = accountData.wasWhitelisted
        set(value) {
            accountData.wasWhitelisted = value
            accountDataStorage.saveAccountData(accountData)
        }
    override var isManualCheckInAllowed: Boolean
        get() = accountData.isManualVisitEnabled
        set(value) {
            accountData.isManualVisitEnabled = value
        }

    override var shouldStartTracking: Boolean
        get() = accountData.shouldUseFirstRunExperienceFlow
        set(value) {
            accountData.shouldUseFirstRunExperienceFlow = value
            accountDataStorage.saveAccountData(accountData)
        }

    suspend fun onKeyReceived(
        key: String,
        checkInEnabled: Boolean? = null,
        pickUpAllowed: Boolean? = null
    ): Boolean {
        val sdk = serviceLocator.getHyperTrackService(key)
        // Log.d(TAG, "HyperTrack deviceId ${sdk.deviceId}")

        val accessTokenRepository = serviceLocator.getAccessTokenRepository(sdk.deviceId, key)
        val accountState = try {
            accessTokenRepository.refreshTokenAsync()
        } catch (ignored: Throwable) {
            Unknown
        }

        var token: String? = null

        when (accountState) {
            Unknown, InvalidCredentials -> return false
            is Active -> token = accountState.token
            Suspended -> {
            } // Log.d(TAG, "Account is suspended or device was deleted")
        }

        isManualCheckInAllowed = checkInEnabled ?: false

        accountDataStorage.saveAccountData(
            AccountData(
                publishableKey = key,
                lastToken = token,
                isManualVisitEnabled = isManualCheckInAllowed,
            )
        )
        accountDataStorage.persistRepository(accessTokenRepository)
        clearLoginAction()
        return true
    }

    companion object {
        const val TAG = "AccountRepo"
    }
}
