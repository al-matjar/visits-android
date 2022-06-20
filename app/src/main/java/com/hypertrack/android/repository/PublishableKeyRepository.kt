package com.hypertrack.android.repository

import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.KeyValueEntry
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toNullableWithErrorReporting

class PublishableKeyRepository(
    private val preferencesRepository: PreferencesRepository,
    private val crashReportsProvider: CrashReportsProvider
) {

    val isLoggedIn: Boolean
        get() = publishableKey.load()
            .toNullableWithErrorReporting(crashReportsProvider) != null

    val publishableKey: KeyValueEntry<RealPublishableKey>
        get() = preferencesRepository.publishableKey

}
