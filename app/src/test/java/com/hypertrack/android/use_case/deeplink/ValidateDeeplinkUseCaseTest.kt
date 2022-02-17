package com.hypertrack.android.use_case.deeplink

import com.hypertrack.android.TestInjector
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.interactors.app.AppReducerTest.Companion.validDeeplinkParams
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.interactors.app.PhoneAuthData
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

@Suppress("ComplexRedundantLet")
class ValidateDeeplinkUseCaseTest {

    @Test
    fun `it should accept valid deeplink params`() {
        runBlocking {
            val publishableKey = "abcd"
            val url = "url"
            validateDeeplinkUseCase().execute(
                validDeeplinkParams(
                    publishableKey = publishableKey,
                    url = "$url?a=c"
                )
            ).collect { result ->
                (result as DeeplinkValid).let {
                    assertEquals(publishableKey, it.userAuthData.publishableKey.value)
                    assertEquals(url, it.deeplinkWithoutGetParams)
                }
            }

            validateDeeplinkUseCase().execute(
                DeeplinkParams(
                    mapOf(
                        ValidateDeeplinkUseCase.DEEPLINK_KEY_PUBLISHABLE_KEY to publishableKey,
                        ValidateDeeplinkUseCase.DEEPLINK_KEY_PHONE to "phone",
                        "~referring_link" to "$url?a=c",
                    )
                )
            ).collect { result ->
                (result as DeeplinkValid).let {
                    assertEquals(publishableKey, it.userAuthData.publishableKey.value)
                    assertEquals(url, it.deeplinkWithoutGetParams)
                }
            }
        }
    }

    @Test
    fun `handle deeplink with email and metadata`() {
        val email = "email@mail.com"
        val metadata = mapOf(
            "a" to 1,
            "b" to "c",
            "c" to mapOf(
                "cc" to 1
            )
        )
        val params = deeplinkParams(
            email = email,
            metadata = metadata
        )
        runBlocking {
            validateDeeplinkUseCase().execute(params).collect { result ->
                (result as DeeplinkValid).let { deeplinkValid ->
                    validate(params, deeplinkValid) {
                        (deeplinkValid.userAuthData as EmailAuthData).let {
                            assertEquals(email, it.email.value)
                            assertEquals(metadata, it.metadata)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `handle deeplink with phone`() {
        val phone = "phone"
        val params = deeplinkParams(
            phone = phone,
        )
        runBlocking {
            validateDeeplinkUseCase().execute(params).collect { result ->
                (result as DeeplinkValid).let { deeplinkValid ->
                    validate(params, deeplinkValid) {
                        (deeplinkValid.userAuthData as PhoneAuthData).let {
                            assertEquals(phone, it.phone.value)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `it should fail on invalid deeplink with phone and email`() {
        runBlocking {
            val publishableKey = "abcd"

            validateDeeplinkUseCase().execute(
                DeeplinkParams(
                    mapOf(
                        "publishable_key" to publishableKey,
                        "email" to "email_11",
                        "phone_number" to "phone_11"
                    )
                )
            ).collect {
                (it as DeeplinkValidationError).let {
                    assertEquals(MultipleLogins, it.failure)
                }
            }
        }
    }

    @Test
    fun `invalid deeplink (no publishable key)`() {
        runBlocking {
            validateDeeplinkUseCase().execute(DeeplinkParams(mapOf("a" to "b"))).collect { result ->
                (result as DeeplinkValidationError).let {
                    assertEquals(NoPublishableKey, it.failure)
                }
            }
        }
    }

    @Test
    fun `old deeplink (just publishable key)`() {
        runBlocking {
            validateDeeplinkUseCase().execute(DeeplinkParams(mapOf("publishable_key" to "key")))
                .collect { result ->
                    (result as DeeplinkValidationError).let {
                        assertEquals(NoLogin, it.failure)
                    }
                }
        }
    }

    @Test
    fun `old deeplink with driver_id`() {
        runBlocking {
            validateDeeplinkUseCase().execute(
                DeeplinkParams(
                    mapOf(
                        "publishable_key" to "key",
                        "driver_id" to "email@mail.com",
                    )
                )
            ).collect { result ->
                (result as DeeplinkValidationError).let {
                    assertEquals(DeprecatedDeeplink, it.failure)
                }
            }
        }
    }


    @Test
    fun `handle deeplink with login fields shadowed in metadata`() {
        DeeplinkParams(
            mapOf(
                "publishable_key" to "key",
                "email" to "email@mail.com",
                "metadata" to mapOf(
                    "email" to "other_email@mail.com"
                )
            )
        ).let {
            runBlocking {
                validateDeeplinkUseCase().execute(it).collect { result ->
                    (result as DeeplinkValidationError).let {
                        assertEquals(MultipleLogins, it.failure)
                    }
                }
            }
        }

        DeeplinkParams(
            mapOf(
                "publishable_key" to "key",
                "phone_number" to "phone",
                "metadata" to mapOf(
                    "phone_number" to "other_phone"
                )
            )
        ).let {
            runBlocking {
                validateDeeplinkUseCase().execute(it).collect { result ->
                    (result as DeeplinkValidationError).let {
                        assertEquals(MultipleLogins, it.failure)
                    }
                }
            }
        }
    }

    companion object {
        fun validateDeeplinkUseCase(): ValidateDeeplinkUseCase {
            return ValidateDeeplinkUseCase(TestInjector.getMoshi())
        }

        fun deeplinkParams(
            email: String? = null,
            phone: String? = null,
            metadata: Map<String, Any>? = null
        ): DeeplinkParams {
            return DeeplinkParams(
                mutableMapOf<String, Any>(
                    "publishable_key" to "key",
                    "~referring_link" to TEST_LINK_URL,
                    "driver_id" to "other_email@mail.com",
                ).apply {
                    metadata?.let { put("metadata", metadata) }
                    email?.let { put("email", email) }
                    phone?.let { put("phone_number", phone) }
                }
            )
        }

        fun validate(
            deeplinkParams: DeeplinkParams,
            deeplinkValid: DeeplinkValid,
            additionalChecks: (DeeplinkValid) -> Unit
        ) {
            assertEquals(TEST_LINK_URL, deeplinkValid.deeplinkWithoutGetParams)
            additionalChecks.invoke(deeplinkValid)
        }

        private const val TEST_LINK_URL = "url"
    }

}
