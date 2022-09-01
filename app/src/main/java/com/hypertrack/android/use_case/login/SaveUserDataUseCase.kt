package com.hypertrack.android.use_case.login

import com.hypertrack.android.models.local.Email
import com.hypertrack.android.models.local.Phone
import com.hypertrack.android.repository.user.UserData
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.sdk.HyperTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.lang.Exception
import java.util.*

@Suppress("OPT_IN_USAGE")
class SaveUserDataUseCase(
    private val hyperTrackSdk: HyperTrack,
    private val userRepository: UserRepository
) {

    fun execute(userData: UserData, deeplinkWithoutGetParams: String?): Flow<SimpleResult> {
        return {
            try {
                userRepository.userData.save(userData)

                val name = userData.metadata?.get("name").stringOrNull()
                    ?: userData.email?.value?.split("@")?.first()?.capitalize(Locale.ROOT)
                    ?: userData.phone?.value

                setDeviceInfo(
                    hyperTrackSdk,
                    name = name,
                    email = userData.email,
                    phoneNumber = userData.phone,
                    metadata = userData.metadata,
                    deeplinkWithoutGetParams = deeplinkWithoutGetParams
                )
                JustSuccess
            } catch (e: Exception) {
                JustFailure(e)
            }
        }.asFlow()
    }

    private fun setDeviceInfo(
        sdkInstance: HyperTrack,
        name: String?,
        email: Email? = null,
        phoneNumber: Phone? = null,
        deeplinkWithoutGetParams: String? = null,
        metadata: Map<String, Any>? = null
    ) {
        sdkInstance.setDeviceName(name)
        sdkInstance.setDeviceMetadata(mutableMapOf<String, Any>().apply {
            email?.value.nullIfBlank()?.let { put(KEY_EMAIL, it) }
            phoneNumber?.value.nullIfBlank()?.let { put(KEY_PHONE, it) }
            deeplinkWithoutGetParams.nullIfBlank()?.let { put(KEY_DEEPLINK, it) }
            metadata?.let { putAll(it) }
        })
    }

    companion object {
        const val KEY_PHONE = "phone_number"
        const val KEY_EMAIL = "email"
        const val KEY_DEEPLINK = "invite_id"
    }

}

fun Any?.stringOrNull(): String? {
    return if (this is String) {
        this
    } else {
        null
    }
}
