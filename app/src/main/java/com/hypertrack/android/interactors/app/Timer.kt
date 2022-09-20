package com.hypertrack.android.interactors.app

sealed class Timer {
    override fun toString(): String = javaClass.simpleName
}
object DeeplinkCheckTimeoutTimer : Timer()
