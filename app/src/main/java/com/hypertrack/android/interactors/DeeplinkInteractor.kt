package com.hypertrack.android.interactors

import android.app.Activity
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.utils.*
import com.squareup.moshi.Moshi
import javax.inject.Provider

class DeeplinkInteractor(
    private val driverRepository: DriverRepository,
    private val accountRepositoryProvider: Provider<AccountRepository>,
    private val crashReportsProvider: CrashReportsProvider,
    private val serviceLocator: ServiceLocator,
    private val moshi: Moshi
) {

    suspend fun handleDeeplink(
        deeplinkResult: DeeplinkResult,
        activity: Activity
    ): HandleDeeplinkResult {
        crashReportsProvider.log("handleDeeplink: $deeplinkResult")
        return when (deeplinkResult) {
            is DeeplinkParams -> {
                deeplinkResult.parameters.let { parameters ->
                    try {
                        parseDeeplink(parameters, activity)
                    } catch (e: Exception) {
                        UserNotLoggedIn(DeeplinkException(e))
                    }
                }
            }
            NoDeeplink -> {
                if (accountRepositoryProvider.get().isLoggedIn) {
                    AlreadyLoggedIn(null)
                } else {
                    UserNotLoggedIn(null)
                }
            }
            is DeeplinkError -> {
                UserNotLoggedIn(DeeplinkException(deeplinkResult.exception))
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun parseDeeplink(
        parameters: Map<String, Any>,
        activity: Activity
    ): HandleDeeplinkResult {
        val publishableKey = parameters["publishable_key"] as String?
        val driverId = parameters["driver_id"] as String?
        val email = parameters["email"] as String?
        val deeplink = parameters["~referring_link"] as String?
        val deeplinkWithoutGetParams = deeplink.urlClearGetParams()
        val phoneNumber = parameters["phone_number"] as String?
        val metadata: Map<String, Any>? = when (val param = parameters["metadata"]) {
            is String -> {
                moshi.createAnyMapAdapter().fromJson(param)
            }
            is Map<*, *> -> param as Map<String, Any>
            else -> null
        }

        return when {
            publishableKey == null -> {
                proceedWithLoggedInCheck(NoPublishableKey)
            }
            email == null && phoneNumber == null && driverId == null -> {
                proceedWithLoggedInCheck(NoLogin)
            }
            metadata?.containsKey("email") == true && email != null ||
                    metadata?.containsKey("phone_number") == true && phoneNumber != null -> {
                proceedWithLoggedInCheck(MultipleLogins)
            }
            else -> {
                validatePublishableKeyAndProceed(
                    publishableKey = publishableKey,
                    driverId = driverId,
                    email = email,
                    phoneNumber = phoneNumber,
                    metadata = metadata,
                    deeplinkWithoutGetParams = deeplinkWithoutGetParams,
                    activity = activity
                )
            }
        }
    }

    private suspend fun validatePublishableKeyAndProceed(
        publishableKey: String,
        driverId: String?,
        email: String?,
        phoneNumber: String?,
        metadata: Map<String, Any>?,
        deeplinkWithoutGetParams: String?,
        activity: Activity
    ): HandleDeeplinkResult {
        check(email != null || phoneNumber != null || driverId != null)
        return try {
            //todo handle connection error
            val correctKey = accountRepositoryProvider.get().onKeyReceived(publishableKey)
            // Log.d(TAG, "onKeyReceived finished")
            if (correctKey) {
                // Log.d(TAG, "Key validated successfully")
                if (driverId != null && (email == null && phoneNumber == null)) {
                    driverRepository.setUserData(
                        driverId = driverId,
                        metadata = metadata,
                        deeplinkWithoutGetParams = deeplinkWithoutGetParams
                    )
                } else {
                    driverRepository.setUserData(
                        email = email,
                        phoneNumber = phoneNumber,
                        //ignored, because this field is sent only as fallback for older versions of Visits app
                        //email is used instead
                        //driverId = driverId,
                        metadata = metadata,
                        deeplinkWithoutGetParams = deeplinkWithoutGetParams
                    )
                }

                crashReportsProvider.setUserIdentifier(
                    moshi.adapter(UserIdentifier::class.java).toJson(
                        UserIdentifier(
                            deviceId = serviceLocator.getHyperTrackService(
                                accountRepositoryProvider.get().publishableKey
                            ).deviceId,
                            driverId = driverRepository.username,
                            pubKey = accountRepositoryProvider.get().publishableKey,
                        )
                    )
                )

                UserLoggedIn
            } else {
                throw Exception("Unable to validate publishable_key")
            }
        } catch (e: Exception) {
            proceedWithLoggedInCheck(DeeplinkException(e))
        }
    }

    private fun proceedWithLoggedInCheck(failure: DeeplinkFailure?): HandleDeeplinkResult {
        return if (accountRepositoryProvider.get().isLoggedIn) {
            AlreadyLoggedIn(failure)
        } else {
            UserNotLoggedIn(failure)
        }
    }


}

sealed class HandleDeeplinkResult
class AlreadyLoggedIn(val failure: DeeplinkFailure?) : HandleDeeplinkResult()
object UserLoggedIn : HandleDeeplinkResult()
class UserNotLoggedIn(val failure: DeeplinkFailure?) : HandleDeeplinkResult()

sealed class DeeplinkFailure
object NoPublishableKey : DeeplinkFailure()
object NoLogin : DeeplinkFailure()
object MultipleLogins : DeeplinkFailure()
class DeeplinkException(val exception: Exception) : DeeplinkFailure()

private fun String?.toBoolean(): Boolean? {
    return when (this) {
        "False", "false" -> false
        "true", "True" -> true
        "", null -> null
        else -> null
    }
}

private fun String?.urlClearGetParams(): String? {
    return this?.split("?")?.first()
}

