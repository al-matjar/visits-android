package com.hypertrack.android.repository.access_token

import com.hypertrack.android.utils.MyApplication

data class UserAccessToken(val value: String) {
    override fun toString(): String {
        return if (MyApplication.DEBUG_MODE) {
            super.toString()
        } else {
            javaClass.simpleName
        }
    }
}
