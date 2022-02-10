package com.hypertrack.android.repository

import com.hypertrack.android.models.local.RealPublishableKey

class PreferencesRepository(
    private val myPreferences: MyPreferences
) {

    val publishableKey: RealPublishableKey?
        get() = myPreferences.getAccountData().publishableKey?.let {
            RealPublishableKey(it)
        }

}
