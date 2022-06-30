@file:Suppress("OPT_IN_USAGE")

package com.hypertrack.android.ui.common.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hypertrack.android.ui.base.Consumable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

fun <T> LiveData<T>.toHotTransformation(): HotLiveDataTransformation<T> {
    return HotLiveDataTransformation(this)
}

class HotLiveDataTransformation<T>(val liveData: LiveData<T>) {
    init {
        liveData.observeForever {}
    }
}

fun <T> LiveData<T>.requireValue(): T {
    return this.value!!
}

fun <T> MutableLiveData<T>.updateValue(value: T) {
    this.value = value
    postValue(value)
}

fun <T> MutableLiveData<Consumable<T>>.postValue(item: T) {
    postValue(Consumable(item))
}

fun <T> LiveData<T>.observeWithErrorHandling(
    lifecycleOwner: LifecycleOwner,
    errorHandler: (Exception) -> Unit,
    observer: (T) -> Unit
) {
    observe(lifecycleOwner) {
        try {
            observer.invoke(it)
        } catch (e: Exception) {
            errorHandler.invoke(e)
        }
    }
}

fun <T> MutableLiveData<T>.updateAsFlow(value: T): Flow<Unit> {
    return { postValue(value) }.asFlow()
}

fun <T> MutableLiveData<Consumable<T>>.updateConsumableAsFlow(value: T): Flow<Unit> {
    return { postValue(value) }.asFlow()
}
