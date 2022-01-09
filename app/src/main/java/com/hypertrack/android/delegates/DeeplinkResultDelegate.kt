package com.hypertrack.android.delegates

import android.app.Activity
import com.hypertrack.android.interactors.*
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.InvalidDeeplinkFormat
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.common.util.nullIfEmpty
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R

abstract class DeeplinkResultDelegate(
    private val deeplinkInteractor: DeeplinkInteractor,
    private val crashReportsProvider: CrashReportsProvider,
    private val resourceProvider: ResourceProvider,
    private val errorHandler: ErrorHandler,
) {

    suspend fun handleDeeplink(result: DeeplinkResult, activity: Activity) {
        deeplinkInteractor.handleDeeplink(result, activity).let { it ->
            when (it) {
                UserLoggedIn -> {
                    proceedToVisitsManagement()
                }
                is AlreadyLoggedIn -> {
                    if (it.failure != null) {
                        handleFailure(it.failure)
                    }
                    proceedToVisitsManagement()
                }
                is UserNotLoggedIn -> {
                    if (it.failure != null) {
                        handleFailure(it.failure)
                    }
                    proceedToSignIn(it)
                }
            } as Unit
        }
    }

    open fun postError(text: String) {
        errorHandler.postText(
            resourceProvider.stringFromResource(
                R.string.splash_screen_invalid_link,
                text
            )
        )
    }

    abstract fun proceedToSignIn(handleDeeplinkResult: HandleDeeplinkResult)

    abstract fun proceedToVisitsManagement()

    private fun handleFailure(failure: DeeplinkFailure) {
        when (failure) {
            is DeeplinkException -> {
                crashReportsProvider.logException(failure.exception)
                when (failure.exception) {
                    is InvalidDeeplinkFormat -> {
                        postError(resourceProvider.stringFromResource(R.string.sign_in_deeplink_invalid_format))
                    }
                    else -> {
                        postError(
                            failure.exception.message.nullIfEmpty()
                                ?: failure.exception.format()
                        )
                    }
                }
            }
            NoPublishableKey -> {
                crashReportsProvider.logException(InvalidDeeplinkException(failure.toString()))
                postError(resourceProvider.stringFromResource(R.string.splash_screen_no_key))
            }
            NoLogin -> {
                crashReportsProvider.logException(InvalidDeeplinkException(failure.toString()))
                postError(resourceProvider.stringFromResource(R.string.splash_screen_no_username))
            }
            MultipleLogins -> {
                crashReportsProvider.logException(InvalidDeeplinkException(failure.toString()))
                postError(resourceProvider.stringFromResource(R.string.splash_screen_duplicate_fields))
            }
        } as Unit
    }

}

class InvalidDeeplinkException(m: String) : Exception(m)
