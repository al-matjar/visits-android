package com.hypertrack.android.utils

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
