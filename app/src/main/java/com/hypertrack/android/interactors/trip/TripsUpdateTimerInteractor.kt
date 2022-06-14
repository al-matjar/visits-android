package com.hypertrack.android.interactors.trip

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// todo tests
// todo appCoroutineScope
class TripsUpdateTimerInteractor(
    val tripsInteractor: TripsInteractor
) {
    private var updateJob: Job? = null
    private val observers = mutableMapOf<String, Boolean>()

    fun registerObserver(id: String) {
        observers[id] = true
        if (updateJob == null) {
            updateJob = GlobalScope.launch {
                while (true) {
                    delay(ETA_UPDATE_PERIOD)
                    tripsInteractor.refreshTrips()
                }
            }
        }
    }

    fun unregisterObserver(id: String) {
        observers.remove(id)
        if (observers.isEmpty()) {
            stopTimer()
        }
    }

    fun onDestroy() {
        stopTimer()
    }

    private fun stopTimer() {
        updateJob?.cancel()
        updateJob = null
    }

    companion object {
        const val ETA_UPDATE_PERIOD = 5 * 60 * 1000L
    }

}
