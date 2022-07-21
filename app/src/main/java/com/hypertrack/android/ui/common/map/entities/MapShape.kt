package com.hypertrack.android.ui.common.map.entities

sealed class MapShape<T>(
    val shape: T
) {
    abstract fun remove()

    override fun toString(): String = javaClass.simpleName
}

