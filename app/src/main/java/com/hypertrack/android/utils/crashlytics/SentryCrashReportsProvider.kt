package com.hypertrack.android.utils.crashlytics

import android.content.Context
import com.hypertrack.android.utils.CrashReportsProvider
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.core.Scope
import io.sentry.core.Sentry
import io.sentry.core.Sentry.OptionsConfiguration
import io.sentry.core.SentryEvent
import io.sentry.core.SentryOptions.BeforeSendCallback
import io.sentry.core.protocol.User
import java.util.*


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
            scope.user = User().apply {
                username = id
            }
        }
    }
}
