package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.EmailAndPhoneAuthData
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.models.local.Phone
import com.hypertrack.android.interactors.app.PhoneAuthData
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.utils.createAnyMapAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class ValidateDeeplinkUseCase(
    private val moshi: Moshi
) {

    fun execute(deeplinkParams: DeeplinkParams): Flow<ValidateDeeplinkResult> {
        return {
            parseDeeplink(deeplinkParams.parameters)
        }.asFlow()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun parseDeeplink(
        parameters: Map<String, Any>,
    ): ValidateDeeplinkResult {
        return try {
            val publishableKey = parameters[DEEPLINK_KEY_PUBLISHABLE_KEY] as String?
            val driverId = parameters[DEEPLINK_KEY_DRIVER_ID] as String?
            //todo validate email
            val email = parameters[DEEPLINK_KEY_EMAIL] as String?
            val deeplink = parameters[DEEPLINK_KEY_LINK] as String?
            val deeplinkWithoutGetParams = deeplink.urlClearGetParams()
            //todo validate phone?
            val phoneNumber = parameters[DEEPLINK_KEY_PHONE] as String?
            val metadata: Map<String, Any>? = when (val param = parameters[DEEPLINK_KEY_METADATA]) {
                is String -> {
                    moshi.createAnyMapAdapter().fromJson(param)
                }
                is Map<*, *> -> param as Map<String, Any>
                else -> null
            }

            when {
                publishableKey == null -> {
                    DeeplinkValidationError(NoPublishableKey)
                }
                email == null && phoneNumber == null && driverId == null -> {
                    DeeplinkValidationError(NoLogin)
                }
                email != null && metadata?.contains(DEEPLINK_KEY_EMAIL) == true ||
                        phoneNumber != null && metadata?.containsKey(DEEPLINK_KEY_PHONE) == true -> {
                    DeeplinkValidationError(MirroredFieldsInMetadata)
                }
                else -> {
                    when {
                        email != null -> {
                            if (phoneNumber != null) {
                                EmailAndPhoneAuthData(
                                    Email(email),
                                    Phone(phoneNumber),
                                    RealPublishableKey(publishableKey),
                                    metadata
                                )
                            } else {
                                EmailAuthData(
                                    Email(email),
                                    RealPublishableKey(publishableKey),
                                    metadata
                                )
                            }.let { DeeplinkValid(it, deeplinkWithoutGetParams) }
                        }
                        phoneNumber != null -> {
                            PhoneAuthData(
                                Phone(phoneNumber),
                                RealPublishableKey(publishableKey),
                                metadata
                            ).let { DeeplinkValid(it, deeplinkWithoutGetParams) }
                        }
                        else -> {
                            DeeplinkValidationError(DeprecatedDeeplink)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            DeeplinkValidationError(DeeplinkException(e))
        }
    }

    companion object {
        const val DEEPLINK_KEY_PUBLISHABLE_KEY = "publishable_key"
        const val DEEPLINK_KEY_DRIVER_ID = "driver_id"
        const val DEEPLINK_KEY_EMAIL = "email"
        const val DEEPLINK_KEY_LINK = "~referring_link"
        const val DEEPLINK_KEY_PHONE = "phone_number"
        const val DEEPLINK_KEY_METADATA = "metadata"
    }
}

private fun String?.urlClearGetParams(): String? {
    return this?.split("?")?.first()
}
