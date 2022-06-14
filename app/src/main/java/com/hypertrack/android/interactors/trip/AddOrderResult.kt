package com.hypertrack.android.interactors.trip

sealed class AddOrderResult
class AddOrderError(val e: Exception) : AddOrderResult()
object AddOrderSuccess : AddOrderResult()
