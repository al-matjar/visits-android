package com.hypertrack.android.utils.crashlytics

import android.util.Log
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.format
import retrofit2.HttpException

class BaseCrashReportsProvider(
    private val crashReportProviderImpl: CrashReportsProvider
) : CrashReportsProvider {

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
            crashReportProviderImpl.logException(exception, metadata)
        }
    }

    override fun log(txt: String) {
        if (MyApplication.DEBUG_MODE) {
            Log.v(javaClass.simpleName + "_log", txt)
        }
        crashReportProviderImpl.log(txt)
    }

    override fun setUserIdentifier(id: String) {
        log("User identifier set: $id")
        crashReportProviderImpl.setUserIdentifier(id)
    }

    override fun setCustomKey(key: String, value: String) {
        crashReportProviderImpl.setCustomKey(key, value)
    }
}
