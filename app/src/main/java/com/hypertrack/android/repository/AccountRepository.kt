package com.hypertrack.android.repository

import android.app.Application
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.hypertrack.android.utils.AccountDataStorage
import com.hypertrack.android.utils.getServiceLocator
import com.hypertrack.sdk.HyperTrack

class AccountRepository(
    private val accountData: AccountData,
    private val accountDataStorage: AccountDataStorage
) {

    val isVerifiedAccount : Boolean
        get() = accountData.lastToken != null

    val publishableKey : String
        get() = accountData.publishableKey ?: ""

    suspend fun onKeyReceived(key: String, application: Application) : Boolean {

        val sdk = HyperTrack.getInstance(application.applicationContext, key)
        Log.d(TAG, "HyperTrack deviceId ${sdk.deviceID}")

        val accessTokenRepository = application.getServiceLocator().getAccessTokenRepository(sdk.deviceID, key)
        val token = accessTokenRepository.refreshTokenAsync()

        if (token.isEmpty()) return false

        accountDataStorage.saveAccountData(AccountData(key, token))
        accountDataStorage.persistRepository(accessTokenRepository)
        return true
    }



    companion object {
        const val TAG = "AccountRepo"
    }
}


data class AccountData(
    @SerializedName("pub_key") val publishableKey : String? = null,
    @SerializedName("last_token") val lastToken : String? = null
)