package com.hypertrack.android.repository.user

import com.hypertrack.android.interactors.app.Email
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.interactors.app.Phone
import com.hypertrack.android.interactors.app.PhoneAuthData
import com.hypertrack.android.interactors.app.UserAuthData
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserData constructor(
    val email: Email?,
    val phone: Phone?,
    val metadata: Map<String, Any>?
) {
    val username: String
        get() = email?.value ?: phone?.value ?: ""

    companion object {
        fun fromUserAuthData(userAuthData: UserAuthData): UserData {
            return when (userAuthData) {
                is EmailAuthData -> UserData(
                    email = userAuthData.email,
                    phone = null,
                    metadata = userAuthData.metadata
                )
                is PhoneAuthData -> {
                    UserData(
                        email = null,
                        phone = userAuthData.phone,
                        metadata = userAuthData.metadata
                    )
                }
            }
        }
    }
}
