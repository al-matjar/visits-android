package com.hypertrack.android.utils

import com.google.android.gms.common.api.ApiException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

// todo rename (remove 'provider')
interface CrashReportsProvider {
    fun logException(exception: Throwable, metadata: Map<String, String> = mapOf())
    fun log(txt: String)
    fun setUserIdentifier(id: String)
    fun setCustomKey(key: String, value: String)

    fun Throwable.shouldBeReported(): Boolean {
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
        is ApiException -> {
            statusCode == 15 || message?.contains("Timeout") == true
        }
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
