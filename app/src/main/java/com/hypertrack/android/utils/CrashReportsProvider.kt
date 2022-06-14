package com.hypertrack.android.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

// todo rename (remove 'provider')
interface CrashReportsProvider {
    fun logException(exception: Throwable, metadata: Map<String, String> = mapOf())
    fun log(txt: String)
    fun setCustomKey(key: String, value: String)
    fun setUserIdentifier(id: String)
}

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

fun Exception.isNetworkError(): Boolean {
    return when (this) {
        is SSLException -> {
            when {
                message?.contains("Software caused connection abort") == true -> {
                    true
                }
                else -> false
            }
        }
        is SocketTimeoutException,
        is UnknownHostException,
        is ConnectException,
        -> true
        else -> false
    }
}

fun tryWithReport(crashReportsProvider: CrashReportsProvider, block: () -> Unit) {
    try {
        block.invoke()
    } catch (e: Exception) {
        crashReportsProvider.logException(e)
    }
}


