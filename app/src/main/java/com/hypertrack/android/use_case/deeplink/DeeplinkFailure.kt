package com.hypertrack.android.use_case.deeplink

sealed class DeeplinkFailure {
    override fun toString(): String = javaClass.simpleName

    fun toException(): Exception {
        val error = this
        return when (error) {
            //todo check error message
            is DeeplinkException -> error.exception
            DeprecatedDeeplink -> InvalidDeeplinkException(error.toString())
            MultipleLogins -> InvalidDeeplinkException(error.toString())
            NoLogin -> InvalidDeeplinkException(error.toString())
            NoPublishableKey -> InvalidDeeplinkException(error.toString())
        }
    }
}

data class DeeplinkException(val exception: Exception) : DeeplinkFailure()
object DeprecatedDeeplink : DeeplinkFailure()
object MultipleLogins : DeeplinkFailure()
object NoLogin : DeeplinkFailure()
object NoPublishableKey : DeeplinkFailure()
