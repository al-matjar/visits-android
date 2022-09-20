package com.hypertrack.android

import com.hypertrack.android.models.local.Email
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.RealPublishableKey
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.use_case.app.AppCreationUseCase
import com.squareup.moshi.Moshi

object TestInjector {

    val TEST_PUBLISHABLE_KEY = RealPublishableKey("publishable_key")
    val TEST_DEVICE_ID = DeviceId("device_id")
    val TEST_USER_TOKEN = UserAccessToken("token")
    val TEST_EMAIL = Email("email")
    val TEST_USER_DATA = UserData(TEST_EMAIL, null, mapOf())
    val TEST_URL = "http://google.com"

    fun getMoshi(): Moshi {
        return AppCreationUseCase.createMoshi()
    }

}
