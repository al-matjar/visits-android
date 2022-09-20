package com.hypertrack.android.utils.crashlytics

import android.content.Context
import com.hypertrack.android.utils.CrashReportsProvider

class DoubleCrashReportsProvider(
    private val sentryCrashReportsProvider: SentryCrashReportsProvider,
    private val firebaseCrashReportsProvider: FirebaseCrashReportsProvider
) : CrashReportsProvider {

    override fun logException(exception: Throwable, metadata: Map<String, String>) {
        sentryCrashReportsProvider.logException(exception, metadata)
        firebaseCrashReportsProvider.logException(exception, metadata)
    }

    override fun log(txt: String) {
        sentryCrashReportsProvider.log(txt)
        firebaseCrashReportsProvider.log(txt)
    }

    override fun setCustomKey(key: String, value: String) {
        sentryCrashReportsProvider.setCustomKey(key, value)
        firebaseCrashReportsProvider.setCustomKey(key, value)
    }

    override fun setUserIdentifier(id: String) {
        sentryCrashReportsProvider.setUserIdentifier(id)
        firebaseCrashReportsProvider.setUserIdentifier(id)
    }
}
