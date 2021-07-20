package com.hypertrack.android.utils

sealed class ResultValue<T>
class ResultSuccess<T>(val value: T) : ResultValue<T>()
class ResultError<T>(val exception: Exception) : ResultValue<T>()

sealed class ResultEmptyValue
class ResultEmptyError<T>(val exception: Exception) : ResultEmptyValue()
object ResultEmptySuccess : ResultEmptyValue()