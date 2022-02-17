package com.hypertrack.android.interactors.app

import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.models.local.RealPublishableKey
import com.squareup.moshi.JsonClass


sealed class UserAuthData(
    val publishableKey: RealPublishableKey,
    val metadata: Map<String, Any>?,
) {
    abstract val username: String
}

@JsonClass(generateAdapter = true)
class EmailAuthData(
    val email: Email,
    publishableKey: RealPublishableKey,
    metadata: Map<String, Any>?,
) : UserAuthData(publishableKey, metadata) {
    override val username: String = email.value
}

@JsonClass(generateAdapter = true)
class PhoneAuthData(
    val phone: Phone,
    publishableKey: RealPublishableKey,
    metadata: Map<String, Any>?,
) : UserAuthData(publishableKey, metadata) {
    override val username: String = phone.value
}

@JsonClass(generateAdapter = true)
class Email(val value: String)

@JsonClass(generateAdapter = true)
class Phone(val value: String)
