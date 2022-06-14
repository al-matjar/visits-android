package com.hypertrack.android.utils.crashlytics

import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.utils.HardwareId
import com.squareup.moshi.JsonClass

// do not place any data that third party can use to identify user
sealed class UserIdentifier

@JsonClass(generateAdapter = true)
data class LoggedInUserIdentifier(val deviceId: String) : UserIdentifier() {
    constructor(deviceIdData: DeviceId) : this(deviceIdData.value)
}

@JsonClass(generateAdapter = true)
data class NotLoggedInUserIdentifier(val hardwareId: String) : UserIdentifier() {
    constructor(hardwareIdData: HardwareId) : this(hardwareIdData.value)
}
