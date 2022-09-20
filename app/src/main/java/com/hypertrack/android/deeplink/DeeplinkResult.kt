package com.hypertrack.android.deeplink

import android.net.Uri

sealed class DeeplinkResult

object NoDeeplink : DeeplinkResult() {
    override fun toString(): String = javaClass.simpleName
}

data class DeeplinkParams(val parameters: Map<String, Any>) : DeeplinkResult()
data class DeeplinkError(
    val exception: Exception,
    // to get deeplink data from backend in case of Branch error
    val deeplinkUri: Uri?
) : DeeplinkResult()
