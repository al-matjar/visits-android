package com.hypertrack.android.utils

interface KeyValueEntry<T> {
    fun save(data: T?): SimpleResult
    fun load(): Result<T?>
}
