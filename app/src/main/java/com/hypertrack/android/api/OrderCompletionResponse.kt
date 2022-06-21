package com.hypertrack.android.api

sealed class OrderCompletionResponse
object OrderCompletionCanceled : OrderCompletionResponse()
object OrderCompletionCompleted : OrderCompletionResponse()
class OrderCompletionFailure(val exception: Exception) : OrderCompletionResponse()
object OrderCompletionSuccess : OrderCompletionResponse()
