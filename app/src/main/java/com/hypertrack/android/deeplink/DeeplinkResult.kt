package com.hypertrack.android.deeplink

sealed class DeeplinkResult

object NoDeeplink : DeeplinkResult() {
    override fun toString(): String = javaClass.simpleName
}

data class DeeplinkParams(val parameters: Map<String, Any>) : DeeplinkResult()
data class DeeplinkError(val exception: Exception) : DeeplinkResult()
