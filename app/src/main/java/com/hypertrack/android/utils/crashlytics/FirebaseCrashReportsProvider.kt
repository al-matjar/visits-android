package com.hypertrack.android.utils.crashlytics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        if (MyApplication.DEBUG_MODE && exception is Exception) {
            Log.v(javaClass.simpleName + "_logged", exception.format())
        }
        if (exception.shouldBeReported()) {
            if (MyApplication.DEBUG_MODE) {
                Log.v(
                    javaClass.simpleName + "_exception",
                    mapOf(
                        "exception" to (exception as Exception).format(),
                        "metadata" to metadata
                    ).toString()
                )
                exception.printStackTrace()
            }
            when (exception) {
                is HttpException -> {
                    // to make sure that the response data is logged to crashlytics
                    log(exception.format())
                    log("Headers: ${exception.response()?.raw()?.request?.headers.toString()}")
                }
                else -> {}
            }
            metadata.forEach {
                FirebaseCrashlytics.getInstance().setCustomKey(it.key, it.value)
            }
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
    }

    override fun setUserIdentifier(id: String) {
        log("User identifier set: $id")
        FirebaseCrashlytics.getInstance().setUserId(id)
    }

    //todo keys to enum
    override fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    //todo keys to enum
    override fun log(txt: String) {
        if (MyApplication.DEBUG_MODE) {
            Log.v(javaClass.simpleName + "_log", txt)
        }
        FirebaseCrashlytics.getInstance().log(txt)
    }

    private fun Throwable.shouldBeReported(): Boolean {
        return if (this is Exception) {
            when {
                this.isNetworkError() -> false
                // todo catch and fix all cases
                this is CancellationException -> false
                else -> true
            }
        } else true
    }

}
