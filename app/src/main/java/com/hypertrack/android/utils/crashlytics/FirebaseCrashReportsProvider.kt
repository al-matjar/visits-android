package com.hypertrack.android.utils.crashlytics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hypertrack.android.repository.access_token.AccountSuspendedException
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.isNetworkError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class FirebaseCrashReportsProvider(appContext: Context) : CrashReportsProvider {
    init {
        FirebaseApp.initializeApp(appContext)
    }

    override fun logException(exception: Throwable, metadata: Map<String, String>) {
        metadata.forEach {
            FirebaseCrashlytics.getInstance().setCustomKey(it.key, it.value)
        }
        FirebaseCrashlytics.getInstance().recordException(exception)
    }

    override fun setUserIdentifier(id: String) {
        FirebaseCrashlytics.getInstance().setUserId(id)
    }

    //todo keys to enum
    override fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    //todo keys to enum
    override fun log(txt: String) {
        FirebaseCrashlytics.getInstance().log(txt)
    }

}
