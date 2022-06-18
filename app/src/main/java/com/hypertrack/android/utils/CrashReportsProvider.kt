package com.hypertrack.android.utils

import android.util.Log
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

// todo rename (remove 'provider')
interface CrashReportsProvider {
    fun logException(e: Throwable, metadata: Map<String, String> = mapOf())
    fun log(txt: String)
    fun setCustomKey(key: String, value: String)
    fun setUserIdentifier(id: String)
}

class FirebaseCrashReportsProvider(appContext: Context) : CrashReportsProvider {
    init {
        FirebaseApp.initializeApp(appContext)
    }

    override fun logException(e: Throwable, metadata: Map<String, String>) {
        if (MyApplication.DEBUG_MODE) {
            Log.v(
                javaClass.simpleName, mapOf(
                    "exception" to (e as Exception).format(),
                    "metadata" to metadata
                ).toString()
            )
            e.printStackTrace()
        }
        if (e.shouldBeReported()) {
            metadata.forEach {
                FirebaseCrashlytics.getInstance().setCustomKey(it.key, it.value)
            }
            FirebaseCrashlytics.getInstance().recordException(e)
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
            Log.v(javaClass.simpleName, txt)
        }
        FirebaseCrashlytics.getInstance().log(txt)
    }

    private fun Throwable.shouldBeReported(): Boolean {
        return if (this is Exception) {
            when {
                this.isNetworkError() ||
                        this is HttpException ||
                        this is NonReportableException
                -> false
                // todo catch and fix all cases
                this is CancellationException -> false
                else -> true
            }
        } else true
    }

}

class NonReportableException(message: String) : Exception(message)

// do not place any data that third party can use to identify user
@JsonClass(generateAdapter = true)
class UserIdentifier(
    val deviceId: String,
)

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
        is HttpException,
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


