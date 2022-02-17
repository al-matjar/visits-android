package com.hypertrack.android.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

class LiveDataObserverManager {

    private val observers = mutableListOf<Pair<LiveData<*>, Observer<*>>>()


    fun add(pair: Pair<LiveData<*>, Observer<*>>) {
        observers.add(pair)
    }

    @Suppress("UNCHECKED_CAST")
    fun onCleared() {
        observers.forEach { it.first.removeObserver(it.second as Observer<Any>) }
    }

}

fun <T> LiveData<T>.observeManaged(
    manager: LiveDataObserverManager,
    observer: Observer<T>
) {
    observeForever(observer)
    manager.add(Pair(this, observer))
}
