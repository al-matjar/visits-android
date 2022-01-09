package com.hypertrack.android.ui.base

import androidx.navigation.NavDirections
import com.google.android.datatransport.runtime.Destination

class Consumable<T>(
    val payload: T,
    private var _consumed: Boolean = false
) {
    val consumed: Boolean
        get() = _consumed

    fun consume(c: (payload: T) -> Unit) {
        if (!consumed) {
            _consumed = true
            c.invoke(payload)
        }
    }

    val value: T?
        get() = if (consumed) {
            throw IllegalStateException("value already consumed")
        } else {
            _consumed = true
            payload
        }

    fun <R> map(mapper: (T) -> R): Consumable<R> {
        return Consumable(mapper.invoke(payload), consumed)
    }
}

fun Exception.toConsumable(): Consumable<Exception> {
    return Consumable(this)
}

fun String.toConsumable(): Consumable<String> {
    return Consumable(this)
}

fun Boolean.toConsumable(): Consumable<Boolean> {
    return Consumable(this)
}

fun NavDirections.toConsumable(): Consumable<NavDirections> {
    return Consumable(this)
}
