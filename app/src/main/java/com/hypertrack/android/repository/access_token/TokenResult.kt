package com.hypertrack.android.repository.access_token

sealed class TokenResult {
    override fun toString(): String = javaClass.simpleName
}

data class Active(val token: String) : TokenResult()
data class Error(val exception: Exception) : TokenResult()
object InvalidCredentials : TokenResult()
object Suspended : TokenResult()
