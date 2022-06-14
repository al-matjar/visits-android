package com.hypertrack.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.hypertrack.android.utils.MyApplication.Companion.context

object DeviceInfoUtils {
    @SuppressLint("HardwareIds")
    fun getHardwareId(context: Context): Result<HardwareId> {
        return tryAsResult {
            HardwareId(
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            )
        }
    }
}

data class HardwareId(val value: String)
