package com.hypertrack.android.repository.user

import com.hypertrack.android.models.local.Email
import com.hypertrack.android.interactors.app.EmailAndPhoneAuthData
import com.hypertrack.android.interactors.app.EmailAuthData
import com.hypertrack.android.models.local.Phone
import com.hypertrack.android.interactors.app.PhoneAuthData
import com.hypertrack.android.interactors.app.UserAuthData
import com.hypertrack.android.utils.MyApplication
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserData constructor(
    val email: Email?,
    val phone: Phone?,
    val metadata: Map<String, Any>?
) {

    override fun toString(): String {
        return if (MyApplication.DEBUG_MODE) {
            super.toString()
        } else {
            javaClass.simpleName
        }
    }

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
                is EmailAndPhoneAuthData -> {
                    UserData(
                        email = userAuthData.email,
                        phone = userAuthData.phone,
                        metadata = userAuthData.metadata
                    )
                }
            }
        }
    }
}
