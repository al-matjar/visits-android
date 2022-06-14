package com.hypertrack.android.interactors.app

import com.hypertrack.android.models.local.PublishableKey
import com.hypertrack.android.models.local.RealPublishableKey
import com.squareup.moshi.JsonClass


sealed class UserAuthData(
    val publishableKey: RealPublishableKey,
    val metadata: Map<String, Any>?,
) {
    abstract val username: String

    override fun toString(): String {
        return "${javaClass.simpleName}(username=$username, publishableKey=$publishableKey, metadata=$metadata)"
    }
}

class EmailAuthData(
    val email: Email,
    publishableKey: RealPublishableKey,
    metadata: Map<String, Any>?,
) : UserAuthData(publishableKey, metadata) {
    override val username: String = email.value
}

class PhoneAuthData(
    val phone: Phone,
    publishableKey: RealPublishableKey,
    metadata: Map<String, Any>?,
) : UserAuthData(publishableKey, metadata) {
    override val username: String = phone.value
}

class EmailAndPhoneAuthData(
    val email: Email,
    val phone: Phone,
    publishableKey: RealPublishableKey,
    metadata: Map<String, Any>?,
) : UserAuthData(publishableKey, metadata) {
    override val username: String = phone.value
}

@JsonClass(generateAdapter = true)
data class Email(val value: String)

@JsonClass(generateAdapter = true)
data class Phone(val value: String)
