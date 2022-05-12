package com.hypertrack.android.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface CrashReportsProvider {
    fun logException(e: Throwable, metadata: Map<String, String> = mapOf())
    fun log(txt: String)
    fun setCustomKey(key: String, value: String)
    fun setUserIdentifier(id: String)
}

class FirebaseCrashReportsProvider : CrashReportsProvider {
    override fun logException(e: Throwable, metadata: Map<String, String>) {
        if (MyApplication.DEBUG_MODE) {
            e.printStackTrace()
        }
        if (e.shouldBeReported()) {
            metadata.forEach {
                FirebaseCrashlytics.getInstance().setCustomKey(it.key, it.value)
            }
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun setUserIdentifier(id: String) = FirebaseCrashlytics.getInstance().setUserId(id)

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
                else -> true
            }
        } else true
    }

}

class NonReportableException(message: String) : Exception(message)

@JsonClass(generateAdapter = true)
class UserIdentifier(
    val deviceId: String,
    val driverId: String,
    val pubKey: String,
)


fun Exception.isNetworkError(): Boolean {
    return when (this) {
        is HttpException,
        is SocketTimeoutException,
        is UnknownHostException,
        is ConnectException,
        -> true
        else -> false
    }
}


