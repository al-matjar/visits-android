package com.hypertrack.android.utils

sealed class ResultValue<T>
class ResultSuccess<T>(val value: T) : ResultValue<T>()
class ResultError<T>(val exception: Exception) : ResultValue<T>()

// todo use in publishable key, device_id
open class TypeWrapper<T>(val value: T)
