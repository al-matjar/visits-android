package com.hypertrack.android.repository.access_token

data class UserAccessToken(val value: String) {
    override fun toString(): String = javaClass.simpleName
}
