package com.hypertrack.android.utils.crashlytics

import com.hypertrack.android.utils.CrashReportsProvider
import io.sentry.core.Scope
import io.sentry.core.Sentry

class SentryCrashReportsProvider : CrashReportsProvider {
    override fun logException(exception: Throwable, metadata: Map<String, String>) {
        Sentry.addBreadcrumb(metadata.toString())
        Sentry.captureException(exception)
    }

    override fun log(txt: String) {
        Sentry.addBreadcrumb(txt)
    }

    override fun setCustomKey(key: String, value: String) {
        Sentry.configureScope { scope: Scope ->
            scope.setContexts(key, value)
        }
    }

    override fun setUserIdentifier(id: String) {
        Sentry.configureScope { scope: Scope ->
            scope.setContexts(USER_ID_KEY, id)
        }
    }

    companion object {
        const val USER_ID_KEY = "hypertrack_user_id"
    }
}
